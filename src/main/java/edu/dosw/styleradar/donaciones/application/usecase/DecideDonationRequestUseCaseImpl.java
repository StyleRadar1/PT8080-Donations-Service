package edu.dosw.styleradar.donaciones.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.donaciones.domain.event.DonationRequestDecidedEvent;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidDonationRequestException;
import edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.domain.model.PublicationStatus;
import edu.dosw.styleradar.donaciones.domain.port.in.DecideDonationRequestUseCase;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationRequestRepositoryPort;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicationRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Implementación del caso de uso de aprobación o rechazo de solicitudes de donación.
 */
@Service
@RequiredArgsConstructor
public class DecideDonationRequestUseCaseImpl implements DecideDonationRequestUseCase {

    private static final Logger log = LoggerFactory.getLogger(DecideDonationRequestUseCaseImpl.class);

    private final DonationRequestRepositoryPort donationRequestRepositoryPort;
    private final PublicationRepositoryPort publicationRepositoryPort;
    private final DonationEventPublisherPort eventPublisherPort;

    @Override
    @Transactional
    public DonationRequest decide(Long foundationId, Long requestId, boolean approve, String reason) {
        log.info("Fundación ID: {} procesa decisión para la solicitud ID: {}. Aprobada: {}",
                foundationId, requestId, approve);

        DonationRequest request = donationRequestRepositoryPort.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe solicitud de donación con ID: " + requestId));

        if (!request.getFoundationId().equals(foundationId)) {
            throw new InvalidDonationRequestException("La solicitud no corresponde a la fundación indicada");
        }

        if (approve) {
            request.approve();
        } else {
            request.reject(reason);
            revertPublicationIfInDonation(request.getPublicationId());
        }

        DonationRequest saved = donationRequestRepositoryPort.save(request);
        log.info("Solicitud ID: {} actualizada con nuevo estado: {}", saved.getId(), saved.getStatus());

        TransactionEventHelper.publishAfterCommitOrImmediate(
                eventPublisherPort,
                new DonationRequestDecidedEvent(
                        saved.getId(),
                        saved.getFoundationId(),
                        saved.getDonorId(),
                        approve,
                        saved.getRejectionReason()
                )
        );

        return saved;
    }

    private void revertPublicationIfInDonation(Long publicationId) {
        if (publicationId == null) {
            return;
        }
        publicationRepositoryPort.findById(publicationId).ifPresent(publication -> {
            if (publication.getStatus() == PublicationStatus.IN_DONATION) {
                publication.revertToAvailableDonation();
                publicationRepositoryPort.save(publication);
                log.info("Publicación ID: {} devuelta a vitrina abierta AVAILABLE tras rechazo", publicationId);
            }
        });
    }
}
