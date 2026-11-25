package edu.dosw.styleradar.donaciones.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import edu.dosw.styleradar.donaciones.domain.event.DonationRequestDecidedEvent;
import edu.dosw.styleradar.donaciones.domain.exception.ExternalServiceUnavailableException;
import edu.dosw.styleradar.donaciones.domain.model.ConservationStatus;
import edu.dosw.styleradar.donaciones.domain.model.DonationOutboxEvent;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequestStatus;
import edu.dosw.styleradar.donaciones.domain.model.GarmentType;
import edu.dosw.styleradar.donaciones.domain.model.LocalidadBogota;
import edu.dosw.styleradar.donaciones.domain.model.OutboxEventStatus;
import edu.dosw.styleradar.donaciones.domain.model.PublicationStatus;
import edu.dosw.styleradar.donaciones.domain.model.Size;
import edu.dosw.styleradar.donaciones.domain.model.Style;
import edu.dosw.styleradar.donaciones.domain.port.in.HandleFoundationDeletedUseCase;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.FoundationCatalogPort;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.in.messaging.FoundationDeletedConsumer;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.in.messaging.event.FoundationDeletedIntegrationEvent;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.entity.DonationOutboxEventEntity;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.entity.DonationRequestEntity;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.entity.SecondHandPublicationEntity;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.repository.SpringDataDonationOutboxRepository;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.repository.SpringDataDonationRequestRepository;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.repository.SpringDataPublicationRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Prueba de integración E2E sobre H2 que verifica el Outbox transaccional de {@code foundation.deleted}:
 * confirma cancelaciones y reversión de prendas antes del envío a RabbitMQ, y garantiza
 * recuperación ante fallo parcial sin duplicar cancelaciones ni avisos ya entregados.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Integración Outbox - Recuperación ante fallo parcial en foundation.deleted")
class FoundationDeletedOutboxIntegrationTest {

    @MockBean
    private DonationEventPublisherPort eventPublisherPort;

    @MockBean
    private FoundationCatalogPort foundationCatalogPort;

    @Autowired
    private FoundationDeletedConsumer foundationDeletedConsumer;

    @Autowired
    private HandleFoundationDeletedUseCase handleFoundationDeletedUseCase;

    @Autowired
    private SpringDataPublicationRepository publicationRepository;

    @Autowired
    private SpringDataDonationRequestRepository donationRequestRepository;

    @Autowired
    private SpringDataDonationOutboxRepository outboxRepository;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
        donationRequestRepository.deleteAll();
        publicationRepository.deleteAll();
    }

    private SecondHandPublicationEntity createInDonationPublication(Long userId, String garmentName) {
        return publicationRepository.save(SecondHandPublicationEntity.builder()
                .userId(userId)
                .garmentName(garmentName)
                .description("Prenda en donación para prueba Outbox")
                .garmentType(GarmentType.CHAQUETAS)
                .brand("Levi's")
                .color("Azul")
                .style(Style.URBAN)
                .size(Size.M)
                .conservationStatus(ConservationStatus.LIKE_NEW)
                .price(BigDecimal.ZERO)
                .esDonacion(true)
                .localidad(LocalidadBogota.CHAPINERO)
                .status(PublicationStatus.IN_DONATION)
                .publishedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    private DonationRequestEntity createActiveRequest(
            SecondHandPublicationEntity publication, Long donorId, Long foundationId, DonationRequestStatus status) {
        return donationRequestRepository.save(DonationRequestEntity.builder()
                .publication(publication)
                .donorId(donorId)
                .foundationId(foundationId)
                .message("Solicitud activa")
                .status(status)
                .requestedAt(LocalDateTime.now())
                .build());
    }

    @Test
    @DisplayName("Con dos solicitudes de la misma fundación: confirma ambas cancelaciones, entrega el primer aviso, falla el segundo y recupera solo el pendiente sin repetir cancelaciones")
    void shouldPersistBothCancellationsAndRecoverOnlyFailedSecondNotificationOnRedelivery() {
        Long foundationId = 501L;
        SecondHandPublicationEntity pub1 = createInDonationPublication(101L, "Chaqueta Uno");
        SecondHandPublicationEntity pub2 = createInDonationPublication(102L, "Chaqueta Dos");
        DonationRequestEntity req1 = createActiveRequest(pub1, 101L, foundationId, DonationRequestStatus.PENDING);
        DonationRequestEntity req2 = createActiveRequest(pub2, 102L, foundationId, DonationRequestStatus.IN_PICKUP);

        AtomicBoolean failSecondNotification = new AtomicBoolean(true);
        doAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg instanceof DonationRequestDecidedEvent event
                    && req2.getId().equals(event.requestId())
                    && failSecondNotification.get()) {
                throw new RuntimeException("RabbitMQ caído al entregar el segundo aviso");
            }
            return null;
        }).when(eventPublisherPort).publishEvent(any());

        FoundationDeletedIntegrationEvent deletedEvent = FoundationDeletedIntegrationEvent.builder()
                .eventId("evt-partial-fail-1")
                .foundationId(foundationId)
                .deletedAt("2026-11-24T21:20:00Z")
                .build();

        // 1. Primera entrega: se cancelan ambas solicitudes en BD, sale el primer aviso y falla el segundo
        assertThatThrownBy(() -> foundationDeletedConsumer.onFoundationDeleted(deletedEvent))
                .isInstanceOf(ExternalServiceUnavailableException.class);

        DonationRequestEntity req1AfterFail = donationRequestRepository.findById(req1.getId()).orElseThrow();
        DonationRequestEntity req2AfterFail = donationRequestRepository.findById(req2.getId()).orElseThrow();
        assertThat(req1AfterFail.getStatus()).isEqualTo(DonationRequestStatus.REJECTED);
        assertThat(req2AfterFail.getStatus()).isEqualTo(DonationRequestStatus.REJECTED);
        assertThat(req1AfterFail.getDecidedAt()).isNotNull();
        assertThat(req2AfterFail.getDecidedAt()).isNotNull();

        assertThat(publicationRepository.findById(pub1.getId()).orElseThrow().getStatus())
                .isEqualTo(PublicationStatus.AVAILABLE);
        assertThat(publicationRepository.findById(pub2.getId()).orElseThrow().getStatus())
                .isEqualTo(PublicationStatus.AVAILABLE);

        List<DonationOutboxEventEntity> outboxAfterFail = outboxRepository.findAll();
        assertThat(outboxAfterFail).hasSize(2);
        DonationOutboxEventEntity outbox1 = outboxAfterFail.stream()
                .filter(e -> e.getRequestId().equals(req1.getId())).findFirst().orElseThrow();
        DonationOutboxEventEntity outbox2 = outboxAfterFail.stream()
                .filter(e -> e.getRequestId().equals(req2.getId())).findFirst().orElseThrow();
        assertThat(outbox1.getStatus()).isEqualTo(OutboxEventStatus.SENT);
        assertThat(outbox1.getAttempts()).isEqualTo(1);
        assertThat(outbox2.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(outbox2.getAttempts()).isEqualTo(1);
        assertThat(outbox2.getLastError()).contains("RabbitMQ caído");

        // 2. Recuperación (redelivery del evento cuando RabbitMQ vuelve): solo debe salir el aviso pendiente de req2
        reset(eventPublisherPort);
        failSecondNotification.set(false);
        foundationDeletedConsumer.onFoundationDeleted(deletedEvent);

        ArgumentCaptor<Object> recoveryCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisherPort, times(1)).publishEvent(recoveryCaptor.capture());
        assertThat(recoveryCaptor.getValue()).isInstanceOf(DonationRequestDecidedEvent.class);
        DonationRequestDecidedEvent recoveredEvent = (DonationRequestDecidedEvent) recoveryCaptor.getValue();
        assertThat(recoveredEvent.requestId()).isEqualTo(req2.getId());
        assertThat(recoveredEvent.approved()).isFalse();

        // Ambas prendas siguen AVAILABLE y ninguna cancelación fue repetida (misma versión y decidedAt)
        assertThat(publicationRepository.findById(pub1.getId()).orElseThrow().getStatus())
                .isEqualTo(PublicationStatus.AVAILABLE);
        assertThat(publicationRepository.findById(pub2.getId()).orElseThrow().getStatus())
                .isEqualTo(PublicationStatus.AVAILABLE);

        DonationRequestEntity req1AfterRecovery = donationRequestRepository.findById(req1.getId()).orElseThrow();
        DonationRequestEntity req2AfterRecovery = donationRequestRepository.findById(req2.getId()).orElseThrow();
        assertThat(req1AfterRecovery.getVersion()).isEqualTo(req1AfterFail.getVersion());
        assertThat(req2AfterRecovery.getVersion()).isEqualTo(req2AfterFail.getVersion());
        assertThat(req1AfterRecovery.getDecidedAt()).isEqualTo(req1AfterFail.getDecidedAt());
        assertThat(req2AfterRecovery.getDecidedAt()).isEqualTo(req2AfterFail.getDecidedAt());

        assertThat(outboxRepository.findByStatusOrderByCreatedAtAscIdAsc(OutboxEventStatus.PENDING)).isEmpty();
        assertThat(outboxRepository.findAll())
                .allMatch(e -> e.getStatus() == OutboxEventStatus.SENT)
                .hasSize(2);
    }

    @Test
    @DisplayName("Debe recuperar aviso pendiente mediante dispatchPendingOutboxEvents sin necesidad de redelivery del broker")
    void shouldRecoverPendingNotificationViaOutboxDispatcher() {
        Long foundationId = 502L;
        SecondHandPublicationEntity pub = createInDonationPublication(103L, "Camisa Lino");
        DonationRequestEntity req = createActiveRequest(pub, 103L, foundationId, DonationRequestStatus.APPROVED);

        AtomicBoolean failBroker = new AtomicBoolean(true);
        doAnswer(invocation -> {
            if (failBroker.get()) {
                throw new RuntimeException("Timeout AMQPS");
            }
            return null;
        }).when(eventPublisherPort).publishEvent(any());

        assertThatThrownBy(() -> handleFoundationDeletedUseCase.handleFoundationDeleted(foundationId))
                .isInstanceOf(ExternalServiceUnavailableException.class);
        assertThat(outboxRepository.findByStatusOrderByCreatedAtAscIdAsc(OutboxEventStatus.PENDING)).hasSize(1);

        reset(eventPublisherPort);
        failBroker.set(false);
        int dispatched = handleFoundationDeletedUseCase.dispatchPendingOutboxEvents();

        assertThat(dispatched).isEqualTo(1);
        verify(eventPublisherPort, times(1)).publishEvent(any(DonationRequestDecidedEvent.class));
        assertThat(outboxRepository.findByStatusOrderByCreatedAtAscIdAsc(OutboxEventStatus.PENDING)).isEmpty();
        assertThat(donationRequestRepository.findById(req.getId()).orElseThrow().getStatus())
                .isEqualTo(DonationRequestStatus.REJECTED);
        assertThat(publicationRepository.findById(pub.getId()).orElseThrow().getStatus())
                .isEqualTo(PublicationStatus.AVAILABLE);
    }
}
