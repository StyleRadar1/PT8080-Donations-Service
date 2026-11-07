package edu.dosw.styleradar.donaciones.domain.event;

import java.time.Instant;

/**
 * Evento emitido cuando una fundación benéfica aprueba o rechaza una solicitud de donación.
 */
public record DonationRequestDecidedEvent(
        Long requestId,
        Long foundationId,
        boolean approved,
        String rejectionReason,
        Instant occurredOn
) {
    public DonationRequestDecidedEvent(Long requestId, Long foundationId, boolean approved, String rejectionReason) {
        this(requestId, foundationId, approved, rejectionReason, Instant.now());
    }
}
