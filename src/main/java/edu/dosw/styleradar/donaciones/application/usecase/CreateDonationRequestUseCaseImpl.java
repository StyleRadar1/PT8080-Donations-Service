package edu.dosw.styleradar.donaciones.application.usecase;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.donaciones.domain.event.DonationRequestCreatedEvent;
import edu.dosw.styleradar.donaciones.domain.exception.DomainConflictException;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidDonationRequestException;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidPublicationException;
import edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequestStatus;
import edu.dosw.styleradar.donaciones.domain.model.FoundationSummary;
import edu.dosw.styleradar.donaciones.domain.model.PublicationStatus;
import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;
import edu.dosw.styleradar.donaciones.domain.port.in.CreateDonationRequestCommand;
import edu.dosw.styleradar.donaciones.domain.port.in.CreateDonationRequestUseCase;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationRequestRepositoryPort;
import edu.dosw.styleradar.donaciones.domain.port.out.FoundationCatalogPort;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicationRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Implementación del caso de uso de radicación y consulta de solicitudes de donación benéfica.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CreateDonationRequestUseCaseImpl implements CreateDonationRequestUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateDonationRequestUseCaseImpl.class);

    private final DonationRequestRepositoryPort donationRequestRepositoryPort;
    private final PublicationRepositoryPort publicationRepositoryPort;
    private final DonationEventPublisherPort eventPublisherPort;
    private final FoundationCatalogPort foundationCatalogPort;

    @Override
    @Transactional
    public DonationRequest create(CreateDonationRequestCommand command) {
        log.info("Creando solicitud de donación del donante: {} para la fundación: {}",
                command.donorId(), command.foundationId());

        SecondHandPublication publication = publicationRepositoryPort.findById(command.publicationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe publicación de segunda mano con ID: " + command.publicationId()));

        if (!publication.getUserId().equals(command.donorId())) {
            throw new InvalidPublicationException("La publicación no pertenece al usuario donante");
        }

        if (publication.getStatus() != PublicationStatus.IN_DONATION) {
            throw new InvalidDonationRequestException(
                    "La prenda debe estar marcada para donación antes de enviar la solicitud. Estado actual: "
                            + publication.getStatus());
        }

        if (donationRequestRepositoryPort.existsActiveRequestForPublication(command.publicationId())) {
            throw new DomainConflictException("La prenda ya tiene una solicitud de donación en curso");
        }

        foundationCatalogPort.validateActiveFoundation(command.foundationId());
        Long representanteId = foundationCatalogPort.findFoundationById(command.foundationId())
                .map(FoundationSummary::representanteId)
                .orElse(null);
        if (representanteId == null) {
            log.warn("No se obtuvo representanteId/fundacionUsuarioId para la fundación ID: {}. "
                    + "ms-notificaciones requiere este campo para enrutar la campana.", command.foundationId());
        }

        DonationRequest request = DonationRequest.builder()
                .publicationId(command.publicationId())
                .donorId(command.donorId())
                .foundationId(command.foundationId())
                .message(command.message())
                .deliveryMode(command.deliveryMode())
                .deliveryPoint(command.deliveryPoint())
                .pickupStartTime(command.startTime())
                .pickupEndTime(command.endTime())
                .build();

        request.initiate();
        DonationRequest saved = donationRequestRepositoryPort.save(request);

        log.info("Solicitud de donación radicada exitosamente con ID: {}", saved.getId());

        String resumenPrenda = publication.getGarment() != null ? publication.getGarment().getName() : null;
        DonationRequestCreatedEvent event = new DonationRequestCreatedEvent(
                saved.getId(),
                saved.getPublicationId(),
                saved.getDonorId(),
                saved.getFoundationId(),
                saved.getStatus() != null ? saved.getStatus().name() : "PENDING",
                representanteId,
                resumenPrenda
        );

        TransactionEventHelper.publishAfterCommitOrImmediate(eventPublisherPort, event);

        return saved;
    }

    @Override
    public List<DonationRequest> getByDonorId(Long donorId) {
        log.info("Consultando historial de donaciones del donante ID: {}", donorId);
        return donationRequestRepositoryPort.findByDonorId(donorId);
    }

    @Override
    public List<DonationRequest> getByFoundationId(Long foundationId, DonationRequestStatus status) {
        log.info("Consultando solicitudes para la fundación ID: {} con filtro de estado: {}", foundationId, status);
        if (status == null) {
            return donationRequestRepositoryPort.findByFoundationId(foundationId);
        }
        return donationRequestRepositoryPort.findByFoundationIdAndStatus(foundationId, status);
    }

    @Override
    public DonationRequest getById(Long id) {
        log.info("Consultando solicitud de donación con ID: {}", id);
        return donationRequestRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe solicitud de donación con ID: " + id));
    }
}
