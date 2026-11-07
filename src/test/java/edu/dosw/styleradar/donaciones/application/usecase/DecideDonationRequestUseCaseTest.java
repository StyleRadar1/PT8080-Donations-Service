package edu.dosw.styleradar.donaciones.application.usecase;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.donaciones.domain.event.DonationRequestDecidedEvent;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidDonationRequestException;
import edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequestStatus;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationRequestRepositoryPort;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para el caso de uso DecideDonationRequestUseCaseImpl.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - DecideDonationRequestUseCaseImpl")
class DecideDonationRequestUseCaseTest {

    @Mock
    private DonationRequestRepositoryPort donationRequestRepositoryPort;

    @Mock
    private DonationEventPublisherPort eventPublisherPort;

    @InjectMocks
    private DecideDonationRequestUseCaseImpl decideDonationRequestUseCase;

    private DonationRequest request;

    @BeforeEach
    void setUp() {
        request = DonationRequest.builder()
                .id(10L)
                .publicationId(100L)
                .donorId(1L)
                .foundationId(20L)
                .status(DonationRequestStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("Debe aprobar solicitud exitosamente")
    void shouldApproveRequestSuccessfully() {
        when(donationRequestRepositoryPort.findById(10L)).thenReturn(Optional.of(request));
        when(donationRequestRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DonationRequest result = decideDonationRequestUseCase.decide(20L, 10L, true, null);

        assertEquals(DonationRequestStatus.APPROVED, result.getStatus());
        verify(donationRequestRepositoryPort).save(request);
        verify(eventPublisherPort).publishEvent(any(DonationRequestDecidedEvent.class));
    }

    @Test
    @DisplayName("Debe rechazar solicitud con motivo y publicar evento")
    void shouldRejectRequestWithReasonAndPublishEvent() {
        when(donationRequestRepositoryPort.findById(10L)).thenReturn(Optional.of(request));
        when(donationRequestRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DonationRequest result = decideDonationRequestUseCase.decide(20L, 10L, false, "Capacidad colmada");

        assertEquals(DonationRequestStatus.REJECTED, result.getStatus());
        assertEquals("Capacidad colmada", result.getRejectionReason());
        verify(eventPublisherPort).publishEvent(any(DonationRequestDecidedEvent.class));
    }

    @Test
    @DisplayName("Debe fallar si la solicitud no existe")
    void shouldFailWhenRequestNotFound() {
        when(donationRequestRepositoryPort.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> decideDonationRequestUseCase.decide(20L, 999L, true, null));
    }

    @Test
    @DisplayName("Debe fallar si la solicitud no corresponde a la fundación")
    void shouldFailWhenFoundationDoesNotMatch() {
        when(donationRequestRepositoryPort.findById(10L)).thenReturn(Optional.of(request));

        assertThrows(InvalidDonationRequestException.class,
                () -> decideDonationRequestUseCase.decide(999L, 10L, true, null));
    }
}
