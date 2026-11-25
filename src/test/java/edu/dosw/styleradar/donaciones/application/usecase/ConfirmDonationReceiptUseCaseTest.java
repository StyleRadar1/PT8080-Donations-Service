package edu.dosw.styleradar.donaciones.application.usecase;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.donaciones.domain.exception.ExternalServiceUnavailableException;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidDonationRequestException;
import edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException;
import edu.dosw.styleradar.donaciones.domain.model.DonationOutboxEvent;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequestStatus;
import edu.dosw.styleradar.donaciones.domain.model.OutboxEventStatus;
import edu.dosw.styleradar.donaciones.domain.model.PublicationStatus;
import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationOutboxRepositoryPort;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationRequestRepositoryPort;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicationRepositoryPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para el caso de uso ConfirmDonationReceiptUseCaseImpl.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - ConfirmDonationReceiptUseCaseImpl")
class ConfirmDonationReceiptUseCaseTest {

    @Mock
    private DonationRequestRepositoryPort donationRequestRepositoryPort;

    @Mock
    private PublicationRepositoryPort publicationRepositoryPort;

    @Mock
    private DonationOutboxRepositoryPort outboxRepositoryPort;

    @Mock
    private DonationOutboxDispatcherService outboxDispatcherService;

    @InjectMocks
    private ConfirmDonationReceiptUseCaseImpl confirmDonationReceiptUseCase;

    private DonationRequest request;
    private SecondHandPublication publication;

    @BeforeEach
    void setUp() {
        request = DonationRequest.builder()
                .id(10L)
                .publicationId(100L)
                .foundationId(20L)
                .donorId(55L)
                .status(DonationRequestStatus.IN_PICKUP)
                .build();

        publication = SecondHandPublication.builder()
                .id(100L)
                .userId(1L)
                .status(PublicationStatus.IN_DONATION)
                .build();
    }

    @Test
    @DisplayName("Debe confirmar recepción, marcar publicación DONATED y encolar 2 eventos Outbox independientes")
    void shouldConfirmReceiptSuccessfully() {
        when(donationRequestRepositoryPort.findById(10L)).thenReturn(Optional.of(request));
        when(publicationRepositoryPort.findById(100L)).thenReturn(Optional.of(publication));
        when(donationRequestRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxRepositoryPort.existsByRequestIdAndEventType(eq(10L), any())).thenReturn(false);

        List<DonationOutboxEvent> savedOutbox = new ArrayList<>();
        when(outboxRepositoryPort.save(any())).thenAnswer(inv -> {
            DonationOutboxEvent evt = inv.getArgument(0);
            savedOutbox.add(evt);
            return evt;
        });

        DonationRequest result = confirmDonationReceiptUseCase.confirmReceipt(20L, 10L);

        assertEquals(DonationRequestStatus.DELIVERED, result.getStatus());
        assertEquals(PublicationStatus.DONATED, publication.getStatus());

        verify(publicationRepositoryPort).save(publication);
        verify(donationRequestRepositoryPort).save(request);
        verify(outboxDispatcherService).dispatchPendingByRequestId(10L);

        assertThat(savedOutbox).hasSize(2);
        assertThat(savedOutbox)
                .extracting(DonationOutboxEvent::getEventType)
                .containsExactly(
                        DonationOutboxEvent.TYPE_DONATION_DELIVERED_NOTIFICATIONS,
                        DonationOutboxEvent.TYPE_DONATION_DELIVERED_STATISTICS);
        assertThat(savedOutbox.get(0).getEventId()).isNotBlank();
        assertThat(savedOutbox.get(1).getEventId()).isNotBlank();
        assertThat(savedOutbox.get(0).getEventId()).isNotEqualTo(savedOutbox.get(1).getEventId());
        assertThat(savedOutbox.get(0).getOccurredAt()).isEqualTo(savedOutbox.get(1).getOccurredAt());
        assertThat(savedOutbox.get(0).getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(savedOutbox.get(1).getStatus()).isEqualTo(OutboxEventStatus.PENDING);
    }

    @Test
    @DisplayName("Debe confirmar entrega aunque falle el despacho inmediato al broker dejando el Outbox pendiente")
    void shouldNotFailDeliveryConfirmationWhenBrokerIsTemporarilyUnavailable() {
        when(donationRequestRepositoryPort.findById(10L)).thenReturn(Optional.of(request));
        when(publicationRepositoryPort.findById(100L)).thenReturn(Optional.of(publication));
        when(donationRequestRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxRepositoryPort.existsByRequestIdAndEventType(eq(10L), any())).thenReturn(false);
        when(outboxRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new ExternalServiceUnavailableException("Broker caído"))
                .when(outboxDispatcherService).dispatchPendingByRequestId(10L);

        DonationRequest result = confirmDonationReceiptUseCase.confirmReceipt(20L, 10L);

        assertEquals(DonationRequestStatus.DELIVERED, result.getStatus());
        verify(outboxDispatcherService).dispatchPendingByRequestId(10L);
    }

    @Test
    @DisplayName("Debe confirmar recepción aunque la publicación no esté en el repositorio")
    void shouldConfirmReceiptWithoutPublication() {
        request.setReceivedAt(LocalDateTime.now(ZoneOffset.UTC));
        when(donationRequestRepositoryPort.findById(10L)).thenReturn(Optional.of(request));
        when(publicationRepositoryPort.findById(100L)).thenReturn(Optional.empty());
        when(donationRequestRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxRepositoryPort.existsByRequestIdAndEventType(eq(10L), any())).thenReturn(true);

        DonationRequest result = confirmDonationReceiptUseCase.confirmReceipt(20L, 10L);

        assertEquals(DonationRequestStatus.DELIVERED, result.getStatus());
        verify(publicationRepositoryPort, never()).save(any());
        verify(donationRequestRepositoryPort).save(request);
        verify(outboxRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Debe fallar si la solicitud no existe sin encolar ni despachar Outbox")
    void shouldFailWhenRequestNotFound() {
        when(donationRequestRepositoryPort.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> confirmDonationReceiptUseCase.confirmReceipt(20L, 999L));
        verifyNoInteractions(outboxRepositoryPort, outboxDispatcherService);
    }

    @Test
    @DisplayName("Debe fallar si la solicitud no corresponde a la fundación sin encolar ni despachar Outbox")
    void shouldFailWhenFoundationDoesNotMatch() {
        when(donationRequestRepositoryPort.findById(10L)).thenReturn(Optional.of(request));

        assertThrows(InvalidDonationRequestException.class,
                () -> confirmDonationReceiptUseCase.confirmReceipt(888L, 10L));
        verifyNoInteractions(outboxRepositoryPort, outboxDispatcherService);
    }
}
