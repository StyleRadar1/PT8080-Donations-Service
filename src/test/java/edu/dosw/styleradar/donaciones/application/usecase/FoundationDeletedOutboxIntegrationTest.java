package edu.dosw.styleradar.donaciones.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
import edu.dosw.styleradar.donaciones.domain.port.out.DonationOutboxRepositoryPort;
import edu.dosw.styleradar.donaciones.domain.port.out.FoundationCatalogPort;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.in.messaging.FoundationDeletedConsumer;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.in.messaging.event.FoundationDeletedIntegrationEvent;
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

    @Autowired
    private DonationOutboxPersistenceAdapter outboxPersistenceAdapter;

    @Autowired
    private FoundationDeletionPersistenceService foundationDeletionPersistenceService;

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
        }).when(eventPublisherPort).publishEventConfirmed(any());

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
        assertThat(List.of(req1AfterFail, req2AfterFail))
                .allMatch(r -> r.getStatus() == DonationRequestStatus.REJECTED && r.getDecidedAt() != null);
        assertThat(publicationRepository.findAllById(List.of(pub1.getId(), pub2.getId())))
                .extracting(SecondHandPublicationEntity::getStatus)
                .containsOnly(PublicationStatus.AVAILABLE);

        List<DonationOutboxEventEntity> outboxAfterFail = outboxRepository.findAll();
        assertThat(outboxAfterFail).hasSize(2);
        DonationOutboxEventEntity outbox1 = outboxAfterFail.stream()
                .filter(e -> e.getRequestId().equals(req1.getId())).findFirst().orElseThrow();
        DonationOutboxEventEntity outbox2 = outboxAfterFail.stream()
                .filter(e -> e.getRequestId().equals(req2.getId())).findFirst().orElseThrow();
        assertThat(outbox1)
                .returns(OutboxEventStatus.SENT, DonationOutboxEventEntity::getStatus)
                .returns(1, DonationOutboxEventEntity::getAttempts);
        assertThat(outbox1.getEventId()).isNotBlank();
        assertThat(outbox2)
                .returns(OutboxEventStatus.PENDING, DonationOutboxEventEntity::getStatus)
                .returns(1, DonationOutboxEventEntity::getAttempts);
        assertThat(outbox2.getEventId()).isNotBlank();
        assertThat(outbox2.getLastError()).contains("RabbitMQ caído");

        // 2. Recuperación (redelivery del evento cuando RabbitMQ vuelve): solo debe salir el aviso pendiente de req2
        reset(eventPublisherPort);
        failSecondNotification.set(false);
        foundationDeletedConsumer.onFoundationDeleted(deletedEvent);

        ArgumentCaptor<Object> recoveryCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisherPort, times(1)).publishEventConfirmed(recoveryCaptor.capture());
        assertThat(recoveryCaptor.getValue()).isInstanceOf(DonationRequestDecidedEvent.class);
        DonationRequestDecidedEvent recoveredEvent = (DonationRequestDecidedEvent) recoveryCaptor.getValue();
        assertThat(recoveredEvent)
                .returns(req2.getId(), DonationRequestDecidedEvent::requestId)
                .returns(outbox2.getEventId(), DonationRequestDecidedEvent::eventId)
                .returns(false, DonationRequestDecidedEvent::approved);

        // Ambas prendas siguen AVAILABLE y ninguna cancelación fue repetida (misma versión y decidedAt)
        assertThat(publicationRepository.findAllById(List.of(pub1.getId(), pub2.getId())))
                .extracting(SecondHandPublicationEntity::getStatus)
                .containsOnly(PublicationStatus.AVAILABLE);

        DonationRequestEntity req1AfterRecovery = donationRequestRepository.findById(req1.getId()).orElseThrow();
        DonationRequestEntity req2AfterRecovery = donationRequestRepository.findById(req2.getId()).orElseThrow();
        assertThat(req1AfterRecovery)
                .returns(req1AfterFail.getVersion(), DonationRequestEntity::getVersion)
                .returns(req1AfterFail.getDecidedAt(), DonationRequestEntity::getDecidedAt);
        assertThat(req2AfterRecovery)
                .returns(req2AfterFail.getVersion(), DonationRequestEntity::getVersion)
                .returns(req2AfterFail.getDecidedAt(), DonationRequestEntity::getDecidedAt);

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
        }).when(eventPublisherPort).publishEventConfirmed(any());

        assertThatThrownBy(() -> handleFoundationDeletedUseCase.handleFoundationDeleted(foundationId))
                .isInstanceOf(ExternalServiceUnavailableException.class);
        assertThat(outboxRepository.findByStatusOrderByCreatedAtAscIdAsc(OutboxEventStatus.PENDING)).hasSize(1);

        reset(eventPublisherPort);
        failBroker.set(false);
        int dispatched = handleFoundationDeletedUseCase.dispatchPendingOutboxEvents();

        assertThat(dispatched).isEqualTo(1);
        verify(eventPublisherPort, times(1)).publishEventConfirmed(any(DonationRequestDecidedEvent.class));
        assertThat(outboxRepository.findByStatusOrderByCreatedAtAscIdAsc(OutboxEventStatus.PENDING)).isEmpty();
        assertThat(donationRequestRepository.findById(req.getId()).orElseThrow().getStatus())
                .isEqualTo(DonationRequestStatus.REJECTED);
        assertThat(publicationRepository.findById(pub.getId()).orElseThrow().getStatus())
                .isEqualTo(PublicationStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Ejecución concurrente entre consumidor y scheduler no debe generar notificaciones duplicadas")
    void shouldNotSendDuplicateNotificationsUnderConcurrentConsumerAndSchedulerExecution() throws Exception {
        Long foundationId = 503L;
        SecondHandPublicationEntity pub1 = createInDonationPublication(201L, "Chaqueta Denim");
        SecondHandPublicationEntity pub2 = createInDonationPublication(202L, "Falda Midi");
        DonationRequestEntity req1 = createActiveRequest(pub1, 201L, foundationId, DonationRequestStatus.PENDING);
        DonationRequestEntity req2 = createActiveRequest(pub2, 202L, foundationId, DonationRequestStatus.IN_PICKUP);

        foundationDeletionPersistenceService.cancelActiveRequestsAndEnqueueOutbox(foundationId);

        List<Long> publishedRequestIds = new CopyOnWriteArrayList<>();
        ConcurrentHashMap<Long, String> publishedEventIdsByRequest = new ConcurrentHashMap<>();
        doAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg instanceof DonationRequestDecidedEvent decided) {
                publishedRequestIds.add(decided.requestId());
                publishedEventIdsByRequest.put(decided.requestId(), decided.eventId());
                java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(60L));
            }
            return null;
        }).when(eventPublisherPort).publishEventConfirmed(any());

        FoundationDeletedIntegrationEvent deletedEvent = FoundationDeletedIntegrationEvent.builder()
                .eventId("evt-concurrent-503")
                .foundationId(foundationId)
                .deletedAt("2026-11-24T22:10:00Z")
                .build();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch startGate = new CountDownLatch(1);
        try {
            Future<Void> consumerFuture = pool.submit(() -> {
                ready.countDown();
                startGate.await(5, TimeUnit.SECONDS);
                foundationDeletedConsumer.onFoundationDeleted(deletedEvent);
                return null;
            });
            Future<Integer> schedulerFuture = pool.submit(() -> {
                ready.countDown();
                startGate.await(5, TimeUnit.SECONDS);
                return handleFoundationDeletedUseCase.dispatchPendingOutboxEvents();
            });

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            startGate.countDown();

            consumerFuture.get(15, TimeUnit.SECONDS);
            int schedulerDispatched = schedulerFuture.get(15, TimeUnit.SECONDS);
            assertThat(schedulerDispatched).isBetween(0, 2);
        } finally {
            pool.shutdownNow();
        }

        assertThat(publishedRequestIds).containsExactlyInAnyOrder(req1.getId(), req2.getId());
        assertThat(publishedEventIdsByRequest).containsKeys(req1.getId(), req2.getId());
        verify(eventPublisherPort, times(2)).publishEventConfirmed(any(DonationRequestDecidedEvent.class));
        assertThat(outboxRepository.findAll())
                .hasSize(2)
                .allMatch(e -> e.getStatus() == OutboxEventStatus.SENT && e.getClaimedBy() == null);
    }

    @Test
    @DisplayName("Debe conservar el mismo eventId si RabbitMQ confirma pero falla actualizar a SENT")
    void shouldPreserveSameEventIdAcrossRetryWhenBrokerAcksButMarkSentFails() {
        Long foundationId = 504L;
        SecondHandPublicationEntity pub = createInDonationPublication(301L, "Suéter Lana");
        DonationRequestEntity req = createActiveRequest(pub, 301L, foundationId, DonationRequestStatus.PENDING);

        foundationDeletionPersistenceService.cancelActiveRequestsAndEnqueueOutbox(foundationId);
        DonationOutboxEventEntity initialEntity = outboxRepository.findAll().stream()
                .filter(e -> e.getRequestId().equals(req.getId()))
                .findFirst()
                .orElseThrow();
        String expectedEventId = initialEntity.getEventId();
        assertThat(expectedEventId).isNotBlank();

        AtomicInteger markSentAttempts = new AtomicInteger(0);
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
                if (markSentAttempts.incrementAndGet() == 1) {
                    throw new RuntimeException("Fallo transitorio actualizando a SENT tras ACK");
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

        // 1. El broker confirma ACK pero falla el update local a SENT
        assertThatThrownBy(dispatcher::dispatchAllPending)
                .isInstanceOf(ExternalServiceUnavailableException.class);

        DonationOutboxEventEntity processingEntity = outboxRepository.findById(initialEntity.getId()).orElseThrow();
        assertThat(processingEntity.getStatus()).isEqualTo(OutboxEventStatus.PROCESSING);
        assertThat(processingEntity.getEventId()).isEqualTo(expectedEventId);
        assertThat(processingEntity.getClaimedBy()).isNotBlank();

        // Mientras el lease siga vigente, otra ejecución no debe duplicar la entrega
        assertThat(dispatcher.dispatchAllPending()).isZero();

        // 2. Expirado el lease, el reintento debe recuperar el registro preservando el mismo eventId
        processingEntity.setClaimedAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(2));
        outboxRepository.save(processingEntity);

        assertThat(dispatcher.dispatchAllPending()).isEqualTo(1);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisherPort, times(2)).publishEventConfirmed(captor.capture());
        List<DonationRequestDecidedEvent> sentEvents = captor.getAllValues().stream()
                .map(DonationRequestDecidedEvent.class::cast)
                .toList();
        assertThat(sentEvents).hasSize(2);
        assertThat(sentEvents.get(0).eventId()).isEqualTo(expectedEventId);
        assertThat(sentEvents.get(1).eventId()).isEqualTo(expectedEventId);

        DonationOutboxEventEntity finalEntity = outboxRepository.findById(initialEntity.getId()).orElseThrow();
        assertThat(finalEntity.getStatus()).isEqualTo(OutboxEventStatus.SENT);
        assertThat(finalEntity.getEventId()).isEqualTo(expectedEventId);
        assertThat(finalEntity.getClaimedBy()).isNull();
        assertThat(finalEntity.getClaimedAt()).isNull();
    }
}
