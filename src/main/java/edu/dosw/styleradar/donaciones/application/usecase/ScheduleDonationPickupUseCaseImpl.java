package edu.dosw.styleradar.donaciones.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import edu.dosw.styleradar.donaciones.domain.event.PickupScheduledEvent;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidDonationRequestException;
import edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.domain.port.in.ScheduleDonationPickupUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.SchedulePickupCommand;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationRequestRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Implementación del caso de uso de agendamiento y coordinación logística de entrega o recogida domiciliaria.
 */
@Service
@RequiredArgsConstructor
public class ScheduleDonationPickupUseCaseImpl implements ScheduleDonationPickupUseCase {

    private static final Logger log = LoggerFactory.getLogger(ScheduleDonationPickupUseCaseImpl.class);

    private final DonationRequestRepositoryPort donationRequestRepositoryPort;
    private final DonationEventPublisherPort eventPublisherPort;

    @Override
    public DonationRequest schedulePickup(SchedulePickupCommand command) {
        log.info("Programando recogida para la solicitud ID: {} por usuario ID: {}",
                command.requestId(), command.userId());

        DonationRequest request = donationRequestRepositoryPort.findById(command.requestId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe solicitud de donación con ID: " + command.requestId()));

        boolean isParticipant = request.getDonorId().equals(command.userId())
                || request.getFoundationId().equals(command.userId());
        if (!isParticipant) {
            throw new InvalidDonationRequestException("El usuario no participa en esta solicitud de donación");
        }

        request.schedulePickup(command.deliveryPoint(), command.startTime(), command.endTime());

        DonationRequest saved = donationRequestRepositoryPort.save(request);
        log.info("Recogida programada con éxito para la solicitud ID: {}", saved.getId());

        eventPublisherPort.publishEvent(
                new PickupScheduledEvent(
                        saved.getId(),
                        saved.getDeliveryPoint(),
                        saved.getPickupStartTime(),
                        saved.getPickupEndTime()
                )
        );

        return saved;
    }
}
