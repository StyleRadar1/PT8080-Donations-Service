package edu.dosw.styleradar.donaciones.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.donaciones.domain.event.DonationRequestDecidedEvent;
import edu.dosw.styleradar.donaciones.domain.exception.ExternalServiceUnavailableException;
import edu.dosw.styleradar.donaciones.domain.model.ConservationStatus;
import edu.dosw.styleradar.donaciones.domain.model.DonationOutboxEvent;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequestStatus;
import edu.dosw.styleradar.donaciones.domain.model.Garment;
import edu.dosw.styleradar.donaciones.domain.model.GarmentType;
import edu.dosw.styleradar.donaciones.domain.model.OutboxEventStatus;
import edu.dosw.styleradar.donaciones.domain.model.PublicationStatus;
import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;
import edu.dosw.styleradar.donaciones.domain.model.Size;
import edu.dosw.styleradar.donaciones.domain.model.Style;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationOutboxRepositoryPort;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationRequestRepositoryPort;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicationRepositoryPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - Outbox y Caso de Uso foundation.deleted")
class HandleFoundationDeletedUseCaseTest {

    @Mock
    private FoundationDeletionPersistenceService persistenceService;

    @Mock
    private DonationOutboxDispatcherService outboxDispatcherService;

    @InjectMocks
    private HandleFoundationDeletedUseCaseImpl useCase;

    private SecondHandPublication donationPublication(Long id, PublicationStatus status) {
        return SecondHandPublication.builder()
                .id(id)
                .userId(101L)
                .garment(Garment.builder()
                        .name("Chaqueta")
                        .type(GarmentType.CHAQUETAS)
                        .brand("Levi's")
                        .color("Azul")
                        .style(Style.URBAN)
                        .build())
                .size(Size.M)
                .conservationStatus(ConservationStatus.LIKE_NEW)
                .price(BigDecimal.ZERO)
                .esDonacion(true)
                .status(status)
                .publishedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Debe orquestar cancelación transaccional + despacho Outbox y soportar dispatchPendingOutboxEvents")
    void shouldOrchestratePersistenceAndOutboxDispatch() {
        assertThat(useCase.handleFoundationDeleted(null)).isZero();
        verifyNoInteractions(persistenceService, outboxDispatcherService);

        when(persistenceService.cancelActiveRequestsAndEnqueueOutbox(3L)).thenReturn(2);
        when(outboxDispatcherService.dispatchPendingByFoundationId(3L)).thenReturn(2);
        when(outboxDispatcherService.dispatchAllPending()).thenReturn(1);

        assertThat(useCase.handleFoundationDeleted(3L)).isEqualTo(2);
        assertThat(useCase.dispatchPendingOutboxEvents()).isEqualTo(1);
        verify(persistenceService).cancelActiveRequestsAndEnqueueOutbox(3L);
        verify(outboxDispatcherService).dispatchPendingByFoundationId(3L);
        verify(outboxDispatcherService).dispatchAllPending();
    }

    @Test
    @DisplayName("FoundationDeletionPersistenceService debe cancelar solicitudes activas, revertir prendas y registrar Outbox sin duplicar")
    void shouldCancelActiveRequestsAndEnqueueOutboxWithoutDuplicates() {
        DonationRequestRepositoryPort reqRepo = mock(DonationRequestRepositoryPort.class);
        PublicationRepositoryPort pubRepo = mock(PublicationRepositoryPort.class);
        DonationOutboxRepositoryPort outboxRepo = mock(DonationOutboxRepositoryPort.class);
        FoundationDeletionPersistenceService service =
                new FoundationDeletionPersistenceService(reqRepo, pubRepo, outboxRepo);

        DonationRequest pending = DonationRequest.builder()
                .id(1L).publicationId(10L).donorId(101L).foundationId(3L)
                .status(DonationRequestStatus.PENDING).requestedAt(LocalDateTime.now()).build();
        DonationRequest approved = DonationRequest.builder()
                .id(2L).publicationId(11L).donorId(102L).foundationId(3L)
                .status(DonationRequestStatus.APPROVED).requestedAt(LocalDateTime.now()).build();
        DonationRequest inPickup = DonationRequest.builder()
                .id(3L).publicationId(null).donorId(103L).foundationId(3L)
                .status(DonationRequestStatus.IN_PICKUP).requestedAt(LocalDateTime.now()).build();

        SecondHandPublication pubInDonation = donationPublication(10L, PublicationStatus.IN_DONATION);
        SecondHandPublication pubAlreadyAvailable = donationPublication(11L, PublicationStatus.AVAILABLE);

        when(reqRepo.findActiveByFoundationId(3L)).thenReturn(List.of(pending, approved, inPickup));
        when(pubRepo.findById(10L)).thenReturn(Optional.of(pubInDonation));
        when(pubRepo.findById(11L)).thenReturn(Optional.of(pubAlreadyAvailable));
        when(reqRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxRepo.existsByRequestIdAndEventType(1L, DonationOutboxEvent.TYPE_DONATION_CANCELLED_FOUNDATION_DELETED))
                .thenReturn(false);
        when(outboxRepo.existsByRequestIdAndEventType(2L, DonationOutboxEvent.TYPE_DONATION_CANCELLED_FOUNDATION_DELETED))
                .thenReturn(false);
        when(outboxRepo.existsByRequestIdAndEventType(3L, DonationOutboxEvent.TYPE_DONATION_CANCELLED_FOUNDATION_DELETED))
                .thenReturn(true);

        int cancelled = service.cancelActiveRequestsAndEnqueueOutbox(3L);

        assertThat(cancelled).isEqualTo(3);
        assertThat(pending.getStatus()).isEqualTo(DonationRequestStatus.REJECTED);
        assertThat(approved.getStatus()).isEqualTo(DonationRequestStatus.REJECTED);
        assertThat(inPickup.getStatus()).isEqualTo(DonationRequestStatus.REJECTED);
        assertThat(pubInDonation.getStatus()).isEqualTo(PublicationStatus.AVAILABLE);

        verify(pubRepo, times(1)).save(pubInDonation);
        verify(pubRepo, never()).save(pubAlreadyAvailable);
        verify(reqRepo, times(3)).save(any());

        ArgumentCaptor<DonationOutboxEvent> outboxCaptor = ArgumentCaptor.forClass(DonationOutboxEvent.class);
        verify(outboxRepo, times(2)).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getAllValues())
                .extracting(DonationOutboxEvent::getRequestId, DonationOutboxEvent::getStatus)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1L, OutboxEventStatus.PENDING),
                        org.assertj.core.groups.Tuple.tuple(2L, OutboxEventStatus.PENDING)
                );

        when(reqRepo.findActiveByFoundationId(99L)).thenReturn(List.of());
        assertThat(service.cancelActiveRequestsAndEnqueueOutbox(99L)).isZero();
    }

    @Test
    @DisplayName("DonationOutboxDispatcherService debe reclamar atómicamente, confirmar ACK y liberar solo si falla el broker")
    void shouldDispatchOutboxEventsIndividuallyAndKeepFailedAsPending() {
        DonationOutboxRepositoryPort outboxRepo = mock(DonationOutboxRepositoryPort.class);
        DonationEventPublisherPort publisher = mock(DonationEventPublisherPort.class);
        DonationOutboxDispatcherService dispatcher = new DonationOutboxDispatcherService(outboxRepo, publisher);

        assertThat(dispatcher.dispatchPendingByFoundationId(null)).isZero();
        when(outboxRepo.findDispatchableByFoundationId(eq(88L), any())).thenReturn(List.of());
        assertThat(dispatcher.dispatchPendingByFoundationId(88L)).isZero();

        DonationOutboxEvent evt1 = DonationOutboxEvent.builder()
                .id(10L).eventId("evt-10").requestId(1L).foundationId(3L).donorId(101L)
                .rejectionReason("Eliminada").eventType(DonationOutboxEvent.TYPE_DONATION_CANCELLED_FOUNDATION_DELETED)
                .status(OutboxEventStatus.PENDING).attempts(0).createdAt(LocalDateTime.now()).build();
        DonationOutboxEvent evt2 = DonationOutboxEvent.builder()
                .id(11L).eventId("evt-11").requestId(2L).foundationId(3L).donorId(102L)
                .rejectionReason("Eliminada").eventType(DonationOutboxEvent.TYPE_DONATION_CANCELLED_FOUNDATION_DELETED)
                .status(OutboxEventStatus.PENDING).attempts(0).createdAt(LocalDateTime.now()).build();

        when(outboxRepo.findDispatchableEvents(any())).thenReturn(List.of(evt1, evt2));
        when(outboxRepo.claimForDispatch(eq(10L), any(), any(), any())).thenReturn(Optional.of(evt1));
        when(outboxRepo.claimForDispatch(eq(11L), any(), any(), any())).thenReturn(Optional.of(evt2));
        doAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg instanceof DonationRequestDecidedEvent d && Long.valueOf(2L).equals(d.requestId())) {
                throw new RuntimeException("Broker down");
            }
            return null;
        }).when(publisher).publishEventConfirmed(any());

        assertThatThrownBy(dispatcher::dispatchAllPending)
                .isInstanceOf(ExternalServiceUnavailableException.class);

        verify(outboxRepo).markSent(eq(10L), any(), any());
        verify(outboxRepo, never()).markSent(eq(11L), any(), any());
        verify(outboxRepo, never()).releaseForRetry(eq(10L), any(), any());
        verify(outboxRepo).releaseForRetry(eq(11L), any(), eq("Broker down"));
    }

    @Test
    @DisplayName("DonationOutboxDispatcherService debe omitir registros ya reclamados atómicamente por otro worker")
    void shouldSkipAlreadyClaimedOutboxEvent() {
        DonationOutboxRepositoryPort outboxRepo = mock(DonationOutboxRepositoryPort.class);
        DonationEventPublisherPort publisher = mock(DonationEventPublisherPort.class);
        DonationOutboxDispatcherService dispatcher = new DonationOutboxDispatcherService(outboxRepo, publisher);

        DonationOutboxEvent evt = DonationOutboxEvent.builder()
                .id(20L).eventId("evt-20").requestId(5L).foundationId(3L).donorId(105L)
                .rejectionReason("Eliminada").eventType(DonationOutboxEvent.TYPE_DONATION_CANCELLED_FOUNDATION_DELETED)
                .status(OutboxEventStatus.PENDING).attempts(0).createdAt(LocalDateTime.now()).build();

        when(outboxRepo.findDispatchableEvents(any())).thenReturn(List.of(evt));
        when(outboxRepo.claimForDispatch(eq(20L), any(), any(), any())).thenReturn(Optional.empty());

        assertThat(dispatcher.dispatchAllPending()).isZero();
        verifyNoInteractions(publisher);
        verify(outboxRepo, never()).markSent(any(), any(), any());
        verify(outboxRepo, never()).releaseForRetry(any(), any(), any());
    }

    @Test
    @DisplayName("DonationOutboxDispatcherService no debe liberar el reclamo ni perder eventId si el broker confirma pero falla markSent")
    void shouldPreserveClaimAndEventIdWhenMarkSentFailsAfterBrokerAck() {
        DonationOutboxRepositoryPort outboxRepo = mock(DonationOutboxRepositoryPort.class);
        DonationEventPublisherPort publisher = mock(DonationEventPublisherPort.class);
        DonationOutboxDispatcherService dispatcher = new DonationOutboxDispatcherService(outboxRepo, publisher);

        DonationOutboxEvent claimed = DonationOutboxEvent.builder()
                .id(30L).eventId("evt-stable-30").requestId(6L).foundationId(3L).donorId(106L)
                .rejectionReason("Eliminada").eventType(DonationOutboxEvent.TYPE_DONATION_CANCELLED_FOUNDATION_DELETED)
                .status(OutboxEventStatus.PROCESSING).attempts(1).createdAt(LocalDateTime.now()).build();

        when(outboxRepo.findDispatchableEvents(any())).thenReturn(List.of(claimed));
        when(outboxRepo.claimForDispatch(eq(30L), any(), any(), any())).thenReturn(Optional.of(claimed));
        doThrow(new RuntimeException("DB lock timeout")).when(outboxRepo).markSent(eq(30L), any(), any());

        assertThatThrownBy(dispatcher::dispatchAllPending)
                .isInstanceOf(ExternalServiceUnavailableException.class);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(publisher).publishEventConfirmed(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(DonationRequestDecidedEvent.class);
        assertThat(((DonationRequestDecidedEvent) captor.getValue()).eventId()).isEqualTo("evt-stable-30");
        verify(outboxRepo, never()).releaseForRetry(any(), any(), any());
    }
}
