package edu.dosw.styleradar.donaciones.application.usecase;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.donaciones.domain.event.PickupScheduledEvent;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidDonationRequestException;
import edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequestStatus;
import edu.dosw.styleradar.donaciones.domain.port.in.SchedulePickupCommand;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationRequestRepositoryPort;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para el caso de uso ScheduleDonationPickupUseCaseImpl.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - ScheduleDonationPickupUseCaseImpl")
class ScheduleDonationPickupUseCaseTest {

    @Mock
    private DonationRequestRepositoryPort donationRequestRepositoryPort;

    @Mock
    private DonationEventPublisherPort eventPublisherPort;

    @InjectMocks
    private ScheduleDonationPickupUseCaseImpl scheduleDonationPickupUseCase;

    private DonationRequest request;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @BeforeEach
    void setUp() {
        request = DonationRequest.builder()
                .id(10L)
                .donorId(1L)
                .foundationId(20L)
                .status(DonationRequestStatus.APPROVED)
                .build();

        startTime = LocalDateTime.now().plusDays(1);
        endTime = startTime.plusHours(2);
    }

    @Test
    @DisplayName("Debe programar recogida exitosamente por el donante y emitir evento")
    void shouldSchedulePickupByDonor() {
        SchedulePickupCommand command = new SchedulePickupCommand(
                1L, 10L, "Calle 100 # 15-20, Bogotá", startTime, endTime
        );

        when(donationRequestRepositoryPort.findById(10L)).thenReturn(Optional.of(request));
        when(donationRequestRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DonationRequest result = scheduleDonationPickupUseCase.schedulePickup(command);

        assertEquals(DonationRequestStatus.IN_PICKUP, result.getStatus());
        assertEquals("Calle 100 # 15-20, Bogotá", result.getDeliveryPoint());
        verify(donationRequestRepositoryPort).save(request);
        verify(eventPublisherPort).publishEvent(any(PickupScheduledEvent.class));
    }

    @Test
    @DisplayName("Debe programar recogida exitosamente por la fundación")
    void shouldSchedulePickupByFoundation() {
        SchedulePickupCommand command = new SchedulePickupCommand(
                20L, 10L, "Sede Fundación", startTime, endTime
        );

        when(donationRequestRepositoryPort.findById(10L)).thenReturn(Optional.of(request));
        when(donationRequestRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DonationRequest result = scheduleDonationPickupUseCase.schedulePickup(command);

        assertEquals(DonationRequestStatus.IN_PICKUP, result.getStatus());
    }

    @Test
    @DisplayName("Debe fallar si la solicitud no existe")
    void shouldFailWhenRequestNotFound() {
        SchedulePickupCommand command = new SchedulePickupCommand(1L, 999L, "Punto", startTime, endTime);
        when(donationRequestRepositoryPort.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> scheduleDonationPickupUseCase.schedulePickup(command));
    }

    @Test
    @DisplayName("Debe fallar si el usuario no participa en la solicitud")
    void shouldFailWhenUserNotParticipant() {
        SchedulePickupCommand command = new SchedulePickupCommand(888L, 10L, "Punto", startTime, endTime);
        when(donationRequestRepositoryPort.findById(10L)).thenReturn(Optional.of(request));

        assertThrows(InvalidDonationRequestException.class,
                () -> scheduleDonationPickupUseCase.schedulePickup(command));
    }
}
