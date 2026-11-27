package edu.dosw.styleradar.donaciones.application.usecase;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import edu.dosw.styleradar.donaciones.domain.event.DonationDeliveredEvent;
import edu.dosw.styleradar.donaciones.domain.event.DonationDeliveredStatisticsEvent;
import edu.dosw.styleradar.donaciones.domain.exception.ExternalServiceUnavailableException;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidStateTransitionException;
import edu.dosw.styleradar.donaciones.domain.model.ConservationStatus;
import edu.dosw.styleradar.donaciones.domain.model.DeliveryMode;
import edu.dosw.styleradar.donaciones.domain.model.DonationOutboxEvent;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequestStatus;
import edu.dosw.styleradar.donaciones.domain.model.GarmentType;
import edu.dosw.styleradar.donaciones.domain.model.LocalidadBogota;
import edu.dosw.styleradar.donaciones.domain.model.OutboxEventStatus;
import edu.dosw.styleradar.donaciones.domain.model.PublicationStatus;
import edu.dosw.styleradar.donaciones.domain.model.Size;
import edu.dosw.styleradar.donaciones.domain.model.Style;
import edu.dosw.styleradar.donaciones.domain.port.in.ConfirmDonationReceiptUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.HandleFoundationDeletedUseCase;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationOutboxRepositoryPort;
import edu.dosw.styleradar.donaciones.domain.port.out.FoundationCatalogPort;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.DonationOutboxPersistenceAdapter;
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
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Pruebas de integración E2E sobre H2 que verifican el Outbox transaccional en entregas confirmadas
 * para la independencia entre {@code ms-notificaciones} y {@code ms-estadisticas}.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Integración Outbox - Entrega confirmada para Notificaciones y Estadísticas")
class DonationDeliveryOutboxIntegrationTest {

    @MockBean
    private DonationEventPublisherPort eventPublisherPort;

    @MockBean
    private FoundationCatalogPort foundationCatalogPort;

    @Autowired
    private ConfirmDonationReceiptUseCase confirmDonationReceiptUseCase;

    @Autowired
    private HandleFoundationDeletedUseCase handleFoundationDeletedUseCase;

    @Autowired
    private SpringDataPublicationRepository publicationRepository;

    @Autowired
    private SpringDataDonationRequestRepository donationRequestRepository;

    @Autowired
    private SpringDataDonationOutboxRepository outboxRepository;

    @Autowired
    private DonationOutboxPersistenceAdapter outboxPersistenceAdapter;

    @Autowired
    private PlatformTransactionManager transactionManager;

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
                .description("Prenda en donación para prueba Outbox entrega")
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

    private DonationRequestEntity createRequest(
            SecondHandPublicationEntity pub, Long donorId, Long foundationId, DonationRequestStatus status) {
        return donationRequestRepository.save(DonationRequestEntity.builder()
                .publication(pub)
                .donorId(donorId)
                .foundationId(foundationId)
                .message("Donación de prueba")
                .deliveryMode(DeliveryMode.DROP_OFF_POINT)
                .deliveryPoint("Punto Central")
                .status(status)
                .requestedAt(LocalDateTime.now())
                .build());
    }

    @Test
    @DisplayName("Debe publicar eventos de Notificaciones y Estadísticas únicamente al confirmar entrega")
    void shouldPublishEventsOnlyWhenDeliveryIsConfirmed() {
        Long foundationId = 701L;
        SecondHandPublicationEntity pub = createInDonationPublication(101L, "Chaqueta Cuero");
        DonationRequestEntity req = createRequest(pub, 101L, foundationId, DonationRequestStatus.IN_PICKUP);

        assertThat(outboxRepository.findAll()).isEmpty();
        verifyNoInteractions(eventPublisherPort);

        confirmDonationReceiptUseCase.confirmReceipt(foundationId, req.getId());

        assertThat(donationRequestRepository.findById(req.getId()).orElseThrow().getStatus())
                .isEqualTo(DonationRequestStatus.DELIVERED);
        assertThat(publicationRepository.findById(pub.getId()).orElseThrow().getStatus())
                .isEqualTo(PublicationStatus.DONATED);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisherPort, times(2)).publishEventConfirmed(captor.capture());

        assertThat(captor.getAllValues()).hasSize(2);
        assertThat(captor.getAllValues().stream().filter(DonationDeliveredEvent.class::isInstance)).hasSize(1);
        assertThat(captor.getAllValues().stream().filter(DonationDeliveredStatisticsEvent.class::isInstance)).hasSize(1);

        List<DonationOutboxEventEntity> outboxRows = outboxRepository.findAll();
        assertThat(outboxRows).hasSize(2)
                .allMatch(e -> e.getStatus() == OutboxEventStatus.SENT && e.getOccurredAt() != null);
    }

    @Test
    @DisplayName("Debe recuperar ambos eventos desde Outbox ante caída total de RabbitMQ al confirmar entrega")
    void shouldRecoverBothEventsWhenBrokerIsDownDuringDeliveryConfirmation() {
        Long foundationId = 702L;
        SecondHandPublicationEntity pub = createInDonationPublication(102L, "Camisa Lino");
        DonationRequestEntity req = createRequest(pub, 102L, foundationId, DonationRequestStatus.IN_PICKUP);

        AtomicBoolean brokerDown = new AtomicBoolean(true);
        doAnswer(invocation -> {
            if (brokerDown.get()) {
                throw new ExternalServiceUnavailableException("RabbitMQ caído");
            }
            return null;
        }).when(eventPublisherPort).publishEventConfirmed(any());

        confirmDonationReceiptUseCase.confirmReceipt(foundationId, req.getId());

        assertThat(donationRequestRepository.findById(req.getId()).orElseThrow().getStatus())
                .isEqualTo(DonationRequestStatus.DELIVERED);
        assertThat(publicationRepository.findById(pub.getId()).orElseThrow().getStatus())
                .isEqualTo(PublicationStatus.DONATED);

        List<DonationOutboxEventEntity> pendingAfterFail =
                outboxRepository.findByStatusOrderByCreatedAtAscIdAsc(OutboxEventStatus.PENDING);
        assertThat(pendingAfterFail).hasSize(2);

        reset(eventPublisherPort);
        brokerDown.set(false);
        int dispatched = handleFoundationDeletedUseCase.dispatchPendingOutboxEvents();

        assertThat(dispatched).isEqualTo(2);
        verify(eventPublisherPort, times(2)).publishEventConfirmed(any());
        assertThat(outboxRepository.findByStatusOrderByCreatedAtAscIdAsc(OutboxEventStatus.PENDING)).isEmpty();
        assertThat(outboxRepository.findAll())
                .hasSize(2)
                .allMatch(e -> e.getStatus() == OutboxEventStatus.SENT);
    }

    @Test
    @DisplayName("Fallo en Estadísticas cuando Notificaciones ya confirmó solo reintenta Estadísticas sin duplicar Notificaciones")
    void shouldRetryOnlyStatisticsWhenNotificationsAlreadySucceeded() {
        Long foundationId = 703L;
        SecondHandPublicationEntity pub = createInDonationPublication(103L, "Abrigo Lana");
        DonationRequestEntity req = createRequest(pub, 103L, foundationId, DonationRequestStatus.IN_PICKUP);

        AtomicBoolean failStats = new AtomicBoolean(true);
        doAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            if (failStats.get() && arg instanceof DonationDeliveredStatisticsEvent) {
                throw new ExternalServiceUnavailableException("Timeout confirmando evento a Estadísticas");
            }
            return null;
        }).when(eventPublisherPort).publishEventConfirmed(any());

        confirmDonationReceiptUseCase.confirmReceipt(foundationId, req.getId());

        List<DonationOutboxEventEntity> outboxAfterPartial = outboxRepository.findAll();
        DonationOutboxEventEntity notifRow = outboxAfterPartial.stream()
                .filter(e -> DonationOutboxEvent.TYPE_DONATION_DELIVERED_NOTIFICATIONS.equals(e.getEventType()))
                .findFirst().orElseThrow();
        DonationOutboxEventEntity statsRow = outboxAfterPartial.stream()
                .filter(e -> DonationOutboxEvent.TYPE_DONATION_DELIVERED_STATISTICS.equals(e.getEventType()))
                .findFirst().orElseThrow();

        assertThat(notifRow.getStatus()).isEqualTo(OutboxEventStatus.SENT);
        assertThat(statsRow.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(statsRow.getLastError()).contains("Estadísticas");

        reset(eventPublisherPort);
        failStats.set(false);

        int dispatched = handleFoundationDeletedUseCase.dispatchPendingOutboxEvents();
        assertThat(dispatched).isEqualTo(1);

        ArgumentCaptor<Object> retryCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisherPort, times(1)).publishEventConfirmed(retryCaptor.capture());
        assertThat(retryCaptor.getValue()).isInstanceOf(DonationDeliveredStatisticsEvent.class);

        DonationDeliveredStatisticsEvent retriedStats = (DonationDeliveredStatisticsEvent) retryCaptor.getValue();
        assertThat(retriedStats.eventId()).isEqualTo(statsRow.getEventId());
        assertThat(retriedStats.occurredOn()).isEqualTo(statsRow.getOccurredAt().toInstant(ZoneOffset.UTC));

        assertThat(outboxRepository.findAll())
                .hasSize(2)
                .allMatch(e -> e.getStatus() == OutboxEventStatus.SENT);
    }

    @Test
    @DisplayName("Debe conservar exactamente el mismo eventId y occurredAt en Estadísticas incluso tras fallo de markSent")
    void shouldPreserveEventIdAndOccurredAtAcrossRetriesForStatistics() {
        Long foundationId = 704L;
        SecondHandPublicationEntity pub = createInDonationPublication(104L, "Botas Cuero");
        DonationRequestEntity req = createRequest(pub, 104L, foundationId, DonationRequestStatus.IN_PICKUP);

        AtomicBoolean failBrokerStats = new AtomicBoolean(true);
        List<DonationDeliveredStatisticsEvent> statsAttempts = new ArrayList<>();
        doAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg instanceof DonationDeliveredStatisticsEvent stats) {
                statsAttempts.add(stats);
                if (failBrokerStats.get()) {
                    throw new ExternalServiceUnavailableException("NACK en broker");
                }
            }
            return null;
        }).when(eventPublisherPort).publishEventConfirmed(any());

        confirmDonationReceiptUseCase.confirmReceipt(foundationId, req.getId());

        DonationOutboxEventEntity statsRow = outboxRepository.findAll().stream()
                .filter(e -> DonationOutboxEvent.TYPE_DONATION_DELIVERED_STATISTICS.equals(e.getEventType()))
                .findFirst().orElseThrow();
        String expectedEventId = statsRow.getEventId();
        Instant expectedOccurredAt = statsRow.getOccurredAt().toInstant(ZoneOffset.UTC);

        // Simulamos un segundo intento donde el broker responde ACK pero falla actualizar a SENT
        failBrokerStats.set(false);
        AtomicInteger markSentCalls = new AtomicInteger(0);
        DonationOutboxRepositoryPort flakyMarkSentRepo = new DonationOutboxRepositoryPort() {
            @Override
            public DonationOutboxEvent save(DonationOutboxEvent event) {
                return outboxPersistenceAdapter.save(event);
            }

            @Override
            public boolean existsByRequestIdAndEventType(Long requestId, String eventType) {
                return outboxPersistenceAdapter.existsByRequestIdAndEventType(requestId, eventType);
            }

            @Override
            public List<DonationOutboxEvent> findDispatchableEvents(LocalDateTime staleBefore) {
                return outboxPersistenceAdapter.findDispatchableEvents(staleBefore);
            }

            @Override
            public List<DonationOutboxEvent> findDispatchableByFoundationId(Long fid, LocalDateTime staleBefore) {
                return outboxPersistenceAdapter.findDispatchableByFoundationId(fid, staleBefore);
            }

            @Override
            public List<DonationOutboxEvent> findDispatchableByRequestId(Long rid, LocalDateTime staleBefore) {
                return outboxPersistenceAdapter.findDispatchableByRequestId(rid, staleBefore);
            }

            @Override
            public Optional<DonationOutboxEvent> claimForDispatch(
                    Long id, String claimToken, LocalDateTime now, LocalDateTime staleBefore) {
                return outboxPersistenceAdapter.claimForDispatch(id, claimToken, now, staleBefore);
            }

            @Override
            public boolean markSent(Long id, String claimToken, LocalDateTime sentAt) {
                if (markSentCalls.incrementAndGet() == 1) {
                    throw new RuntimeException("Timeout BD actualizando a SENT");
                }
                return outboxPersistenceAdapter.markSent(id, claimToken, sentAt);
            }

            @Override
            public void releaseForRetry(Long id, String claimToken, String errorMessage) {
                outboxPersistenceAdapter.releaseForRetry(id, claimToken, errorMessage);
            }
        };

        DonationOutboxDispatcherService dispatcher = new DonationOutboxDispatcherService(
                flakyMarkSentRepo, eventPublisherPort, 60L);

        assertThatThrownBy(dispatcher::dispatchAllPending)
                .isInstanceOf(ExternalServiceUnavailableException.class);

        DonationOutboxEventEntity processingRow = outboxRepository.findById(statsRow.getId()).orElseThrow();
        processingRow.setClaimedAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(2));
        outboxRepository.save(processingRow);

        assertThat(dispatcher.dispatchAllPending()).isEqualTo(1);

        assertThat(statsAttempts)
                .hasSize(3)
                .allSatisfy(evt -> {
                    assertThat(evt.eventId()).isEqualTo(expectedEventId);
                    assertThat(evt.occurredOn()).isEqualTo(expectedOccurredAt);
                });
        assertThat(outboxRepository.findById(statsRow.getId()).orElseThrow().getStatus())
                .isEqualTo(OutboxEventStatus.SENT);
    }

    @Test
    @DisplayName("No debe registrar ni publicar eventos si la transacción de entrega falla o se revierte")
    void shouldPublishNothingWhenDeliveryTransactionRollsBack() {
        Long foundationId = 705L;
        SecondHandPublicationEntity pub = createInDonationPublication(105L, "Pantalón Drill");
        DonationRequestEntity invalidStateReq = createRequest(pub, 105L, foundationId, DonationRequestStatus.APPROVED);

        Long invalidStateReqId = invalidStateReq.getId();
        assertThatThrownBy(() -> confirmDonationReceiptUseCase.confirmReceipt(foundationId, invalidStateReqId))
                .isInstanceOf(InvalidStateTransitionException.class);

        SecondHandPublicationEntity pubValid = createInDonationPublication(106L, "Vestido Lino");
        DonationRequestEntity validReq = createRequest(pubValid, 106L, foundationId, DonationRequestStatus.IN_PICKUP);

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.executeWithoutResult(status -> {
            confirmDonationReceiptUseCase.confirmReceipt(foundationId, validReq.getId());
            status.setRollbackOnly();
        });

        assertThat(donationRequestRepository.findById(validReq.getId()).orElseThrow().getStatus())
                .isEqualTo(DonationRequestStatus.IN_PICKUP);
        assertThat(publicationRepository.findById(pubValid.getId()).orElseThrow().getStatus())
                .isEqualTo(PublicationStatus.IN_DONATION);
        assertThat(outboxRepository.findAll()).isEmpty();
        verifyNoInteractions(eventPublisherPort);
    }
}
