package edu.dosw.styleradar.donaciones.domain.event;

import java.time.Instant;

/**
 * Evento emitido cuando una fundación benéfica aprueba o rechaza una solicitud de donación.
 */
public record DonationRequestDecidedEvent(
        Long requestId,
        Long foundationId,
        Long donorId,
        boolean approved,
        String rejectionReason,
        Instant occurredOn,
        String eventId
) {
    public DonationRequestDecidedEvent(Long requestId, Long foundationId, boolean approved, String rejectionReason) {
        this(requestId, foundationId, null, approved, rejectionReason, Instant.now(), null);
    }

    public DonationRequestDecidedEvent(Long requestId, Long foundationId, Long donorId, boolean approved, String rejectionReason) {
        this(requestId, foundationId, donorId, approved, rejectionReason, Instant.now(), null);
    }

    public DonationRequestDecidedEvent(
            Long requestId,
            Long foundationId,
            Long donorId,
            boolean approved,
            String rejectionReason,
            Instant occurredOn) {
        this(requestId, foundationId, donorId, approved, rejectionReason, occurredOn, null);
    }
}
