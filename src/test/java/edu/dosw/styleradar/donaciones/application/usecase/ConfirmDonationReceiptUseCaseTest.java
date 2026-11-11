package edu.dosw.styleradar.donaciones.application.usecase;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.donaciones.domain.event.DonationDeliveredEvent;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidDonationRequestException;
import edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequestStatus;
import edu.dosw.styleradar.donaciones.domain.model.PublicationStatus;
import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationRequestRepositoryPort;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicationRepositoryPort;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    private DonationEventPublisherPort eventPublisherPort;

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
                .status(DonationRequestStatus.IN_PICKUP)
                .build();

        publication = SecondHandPublication.builder()
                .id(100L)
                .userId(1L)
                .status(PublicationStatus.IN_DONATION)
                .build();
    }

    @Test
    @DisplayName("Debe confirmar recepción, marcar publicación DONATED y emitir evento")
    void shouldConfirmReceiptSuccessfully() {
        when(donationRequestRepositoryPort.findById(10L)).thenReturn(Optional.of(request));
        when(publicationRepositoryPort.findById(100L)).thenReturn(Optional.of(publication));
        when(donationRequestRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DonationRequest result = confirmDonationReceiptUseCase.confirmReceipt(20L, 10L);

        assertEquals(DonationRequestStatus.DELIVERED, result.getStatus());
        assertEquals(PublicationStatus.DONATED, publication.getStatus());

        verify(publicationRepositoryPort).save(publication);
        verify(donationRequestRepositoryPort).save(request);
        verify(eventPublisherPort).publishEvent(any(DonationDeliveredEvent.class));
    }

    @Test
    @DisplayName("Debe confirmar recepción aunque la publicación no esté en el repositorio")
    void shouldConfirmReceiptWithoutPublication() {
        when(donationRequestRepositoryPort.findById(10L)).thenReturn(Optional.of(request));
        when(publicationRepositoryPort.findById(100L)).thenReturn(Optional.empty());
        when(donationRequestRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DonationRequest result = confirmDonationReceiptUseCase.confirmReceipt(20L, 10L);

        assertEquals(DonationRequestStatus.DELIVERED, result.getStatus());
        verify(publicationRepositoryPort, never()).save(any());
        verify(donationRequestRepositoryPort).save(request);
    }

    @Test
    @DisplayName("Debe fallar si la solicitud no existe")
    void shouldFailWhenRequestNotFound() {
        when(donationRequestRepositoryPort.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> confirmDonationReceiptUseCase.confirmReceipt(20L, 999L));
    }

    @Test
    @DisplayName("Debe fallar si la solicitud no corresponde a la fundación")
    void shouldFailWhenFoundationDoesNotMatch() {
        when(donationRequestRepositoryPort.findById(10L)).thenReturn(Optional.of(request));

        assertThrows(InvalidDonationRequestException.class,
                () -> confirmDonationReceiptUseCase.confirmReceipt(888L, 10L));
    }
}
