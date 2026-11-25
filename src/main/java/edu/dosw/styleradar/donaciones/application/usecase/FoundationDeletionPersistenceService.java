package edu.dosw.styleradar.donaciones.application.usecase;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.donaciones.domain.model.DonationOutboxEvent;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.domain.model.PublicationStatus;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationOutboxRepositoryPort;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationRequestRepositoryPort;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicationRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Servicio transaccional que cancela solicitudes activas de una fundación eliminada,
 * revierte sus prendas a {@code AVAILABLE} y registra en la misma transacción
 * los avisos pendientes en el Outbox identificados por {@code solicitudId}.
 */
@Service
@RequiredArgsConstructor
public class FoundationDeletionPersistenceService {

    static final String CANCELLATION_REASON = "La fundación destinataria fue eliminada de la plataforma";

    private static final Logger log = LoggerFactory.getLogger(FoundationDeletionPersistenceService.class);

    private final DonationRequestRepositoryPort donationRequestRepositoryPort;
    private final PublicationRepositoryPort publicationRepositoryPort;
    private final DonationOutboxRepositoryPort outboxRepositoryPort;

    @Transactional
    public int cancelActiveRequestsAndEnqueueOutbox(Long foundationId) {
        List<DonationRequest> activeRequests = donationRequestRepositoryPort.findActiveByFoundationId(foundationId);
        if (activeRequests.isEmpty()) {
            log.info("No existen solicitudes activas para la fundación eliminada ID: {} (sin cambios lógicos).",
                    foundationId);
            return 0;
        }

        int cancelledCount = 0;
        for (DonationRequest request : activeRequests) {
            request.cancelDueToFoundationDeletion(CANCELLATION_REASON);
            revertPublicationIfInDonation(request.getPublicationId());
            DonationRequest saved = donationRequestRepositoryPort.save(request);
            enqueueOutboxIfAbsent(saved);
            cancelledCount++;
        }

        log.info("Confirmadas {} cancelaciones con registro Outbox para la fundación eliminada ID: {}.",
                cancelledCount, foundationId);
        return cancelledCount;
    }

    private void enqueueOutboxIfAbsent(DonationRequest savedRequest) {
        if (!outboxRepositoryPort.existsByRequestIdAndEventType(
                savedRequest.getId(), DonationOutboxEvent.TYPE_DONATION_CANCELLED_FOUNDATION_DELETED)) {
            outboxRepositoryPort.save(DonationOutboxEvent.pendingFoundationCancellation(savedRequest));
        }
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
