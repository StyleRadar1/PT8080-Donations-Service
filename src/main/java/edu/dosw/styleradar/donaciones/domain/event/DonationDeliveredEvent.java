package edu.dosw.styleradar.donaciones.domain.event;

import java.time.Instant;

/**
 * Evento emitido cuando la fundación confirma la recepción física y entrega de la prenda donada.
 */
public record DonationDeliveredEvent(
        Long requestId,
        Long publicationId,
        Long foundationId,
        Instant occurredOn
) {
    public DonationDeliveredEvent(Long requestId, Long publicationId, Long foundationId) {
        this(requestId, publicationId, foundationId, Instant.now());
    }
}
