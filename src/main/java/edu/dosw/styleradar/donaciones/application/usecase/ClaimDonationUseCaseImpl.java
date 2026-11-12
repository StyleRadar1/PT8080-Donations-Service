package edu.dosw.styleradar.donaciones.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.donaciones.domain.event.DonationRequestCreatedEvent;
import edu.dosw.styleradar.donaciones.domain.exception.ConcurrentDonationClaimException;
import edu.dosw.styleradar.donaciones.domain.exception.DomainConflictException;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidDonationRequestException;
import edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.domain.model.PublicationStatus;
import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;
import edu.dosw.styleradar.donaciones.domain.port.in.ClaimDonationCommand;
import edu.dosw.styleradar.donaciones.domain.port.in.ClaimDonationUseCase;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationRequestRepositoryPort;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicationRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Implementación del caso de uso de reclamo de donaciones en la Vitrina Abierta benéfica.
 * Asegura control de concurrencia optimista y previene auto-donaciones.
 */
@Service
@RequiredArgsConstructor
public class ClaimDonationUseCaseImpl implements ClaimDonationUseCase {

    private static final Logger log = LoggerFactory.getLogger(ClaimDonationUseCaseImpl.class);

    private final PublicationRepositoryPort publicationRepositoryPort;
    private final DonationRequestRepositoryPort donationRequestRepositoryPort;
    private final DonationEventPublisherPort eventPublisherPort;

    @Override
    @Transactional
    public DonationRequest claimDonation(ClaimDonationCommand command) {
        if (command == null || command.publicationId() == null || command.foundationId() == null) {
            throw new InvalidDonationRequestException("El ID de publicación y el ID de la fundación son obligatorios");
        }

        log.info("Fundación ID: {} intenta reclamar prenda ID: {} en la Vitrina Abierta",
                command.foundationId(), command.publicationId());

        SecondHandPublication publication = publicationRepositoryPort.findById(command.publicationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe publicación con ID: " + command.publicationId()));

        if (!publication.isEsDonacion()) {
            throw new InvalidDonationRequestException(
                    "La prenda solicitada no está marcada para donación benéfica (es de reventa de segunda mano)");
        }

        if (publication.getStatus() != PublicationStatus.AVAILABLE) {
            throw new DomainConflictException(
                    "La donación ya fue reclamada o no está disponible en la vitrina. Estado actual: "
                            + publication.getStatus());
        }

        if (publication.getUserId().equals(command.foundationId())) {
            throw new InvalidDonationRequestException(
                    "Regla de negocio: Una fundación no puede reclamar o aceptar su propia donación");
        }

        if (donationRequestRepositoryPort.existsActiveRequestForPublication(command.publicationId())) {
            throw new DomainConflictException("La prenda ya tiene una solicitud o reclamo de donación en curso");
        }

        try {
            publication.markForDonation();
            publicationRepositoryPort.save(publication);
        } catch (OptimisticLockingFailureException ex) {
            log.warn("Conflicto de concurrencia detectado al reclamar publicación ID: {}", command.publicationId());
            throw new ConcurrentDonationClaimException(
                    "La prenda ya fue reclamada concurrentemente por otra fundación o cambió de estado.", ex);
        }

        DonationRequest request = new DonationRequest();
        request.claim(publication.getId(), publication.getUserId(), command.foundationId(), command.message());
        DonationRequest savedRequest = donationRequestRepositoryPort.save(request);

        log.info("Donación ID: {} reclamada exitosamente por fundación ID: {}. Solicitud generada: {}",
                publication.getId(), command.foundationId(), savedRequest.getId());

        eventPublisherPort.publishEvent(new DonationRequestCreatedEvent(
                savedRequest.getId(),
                savedRequest.getPublicationId(),
                savedRequest.getDonorId(),
                savedRequest.getFoundationId(),
                savedRequest.getStatus() != null ? savedRequest.getStatus().name() : "APPROVED"
        ));

        return savedRequest;
    }
}
