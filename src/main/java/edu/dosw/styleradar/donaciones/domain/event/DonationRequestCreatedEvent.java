package edu.dosw.styleradar.donaciones.domain.event;

import java.time.Instant;

/**
 * Evento emitido cuando se genera una solicitud de donación (radicada directamente o reclamada en Vitrina Abierta).
 */
public record DonationRequestCreatedEvent(
        Long requestId,
        Long publicationId,
        Long donorId,
        Long foundationId,
        String status,
        Instant occurredOn
) {
    public DonationRequestCreatedEvent(Long requestId, Long publicationId, Long donorId, Long foundationId, String status) {
        this(requestId, publicationId, donorId, foundationId, status, Instant.now());
    }

    public DonationRequestCreatedEvent(Long requestId, Long publicationId, Long donorId, Long foundationId, Instant occurredOn) {
        this(requestId, publicationId, donorId, foundationId, "PENDING", occurredOn);
    }

    public DonationRequestCreatedEvent(Long requestId, Long publicationId, Long donorId, Long foundationId) {
        this(requestId, publicationId, donorId, foundationId, "PENDING", Instant.now());
    }
}
