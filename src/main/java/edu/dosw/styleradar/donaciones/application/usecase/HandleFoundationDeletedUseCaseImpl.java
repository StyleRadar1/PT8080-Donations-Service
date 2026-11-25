package edu.dosw.styleradar.donaciones.application.usecase;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.donaciones.domain.event.DonationRequestDecidedEvent;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.domain.model.PublicationStatus;
import edu.dosw.styleradar.donaciones.domain.port.in.HandleFoundationDeletedUseCase;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationRequestRepositoryPort;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicationRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Caso de uso que reacciona al evento {@code foundation.deleted} de ms-tiendas,
 * cerrando solicitudes activas ({@code PENDING}, {@code APPROVED}, {@code IN_PICKUP})
 * y devolviendo las publicaciones a la Vitrina Abierta ({@code AVAILABLE}).
 */
@Service
@RequiredArgsConstructor
public class HandleFoundationDeletedUseCaseImpl implements HandleFoundationDeletedUseCase {

    static final String CANCELLATION_REASON = "La fundación destinataria fue eliminada de la plataforma";

    private static final Logger log = LoggerFactory.getLogger(HandleFoundationDeletedUseCaseImpl.class);

    private final DonationRequestRepositoryPort donationRequestRepositoryPort;
    private final PublicationRepositoryPort publicationRepositoryPort;
    private final DonationEventPublisherPort eventPublisherPort;

    @Override
    @Transactional
    public int handleFoundationDeleted(Long foundationId) {
        if (foundationId == null) {
            log.warn("Se omitió procesamiento de eliminación de fundación por foundationId nulo.");
            return 0;
        }

        List<DonationRequest> activeRequests = donationRequestRepositoryPort.findActiveByFoundationId(foundationId);
        if (activeRequests.isEmpty()) {
            log.info("No existen solicitudes activas para la fundación eliminada ID: {} (procesamiento idempotente).",
                    foundationId);
            return 0;
        }

        int cancelledCount = 0;
        for (DonationRequest request : activeRequests) {
            request.cancelDueToFoundationDeletion(CANCELLATION_REASON);
            revertPublicationIfInDonation(request.getPublicationId());
            DonationRequest saved = donationRequestRepositoryPort.save(request);
            cancelledCount++;

            TransactionEventHelper.publishAfterCommitOrImmediate(
                    eventPublisherPort,
                    new DonationRequestDecidedEvent(
                            saved.getId(),
                            saved.getFoundationId(),
                            saved.getDonorId(),
                            false,
                            saved.getRejectionReason()
                    )
            );
        }

        log.info("Cerradas {} solicitudes activas tras la eliminación de la fundación ID: {}.",
                cancelledCount, foundationId);
        return cancelledCount;
    }

    private void revertPublicationIfInDonation(Long publicationId) {
        if (publicationId == null) {
            return;
        }
        publicationRepositoryPort.findById(publicationId).ifPresent(publication -> {
            if (publication.getStatus() == PublicationStatus.IN_DONATION) {
                publication.revertToAvailableDonation();
                publicationRepositoryPort.save(publication);
                log.info("Publicación ID: {} devuelta a vitrina abierta AVAILABLE tras eliminación de fundación.",
                        publicationId);
            }
        });
    }
}
