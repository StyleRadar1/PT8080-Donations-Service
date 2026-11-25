package edu.dosw.styleradar.donaciones.domain.event;

import java.time.Instant;

/**
 * Evento emitido cuando la fundación confirma la recepción física y entrega de la prenda donada.
 */
public record DonationDeliveredEvent(
        Long requestId,
        Long publicationId,
        Long foundationId,
        Long donorId,
        Instant occurredOn
) {
    public DonationDeliveredEvent(Long requestId, Long publicationId, Long foundationId) {
        this(requestId, publicationId, foundationId, null, Instant.now());
    }

    public DonationDeliveredEvent(Long requestId, Long publicationId, Long foundationId, Long donorId) {
        this(requestId, publicationId, foundationId, donorId, Instant.now());
    }
}
