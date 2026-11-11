package edu.dosw.styleradar.donaciones.domain.event;

import java.time.Instant;

/**
 * Evento emitido cuando un donante postula una prenda publicada para donación a una fundación.
 */
public record DonationRequestCreatedEvent(
        Long requestId,
        Long publicationId,
        Long donorId,
        Long foundationId,
        Instant occurredOn
) {
    public DonationRequestCreatedEvent(Long requestId, Long publicationId, Long donorId, Long foundationId) {
        this(requestId, publicationId, donorId, foundationId, Instant.now());
    }
}
