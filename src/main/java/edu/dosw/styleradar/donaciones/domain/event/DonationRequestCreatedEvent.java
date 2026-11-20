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
        Instant occurredOn,
        Long representanteId,
        String resumen
) {
    public DonationRequestCreatedEvent(
            Long requestId,
            Long publicationId,
            Long donorId,
            Long foundationId,
            String status,
            Instant occurredOn,
            Long representanteId) {
        this(requestId, publicationId, donorId, foundationId, status, occurredOn, representanteId, null);
    }

    public DonationRequestCreatedEvent(
            Long requestId,
            Long publicationId,
            Long donorId,
            Long foundationId,
            String status,
            Instant occurredOn) {
        this(requestId, publicationId, donorId, foundationId, status, occurredOn, null, null);
    }

    public DonationRequestCreatedEvent(
            Long requestId,
            Long publicationId,
            Long donorId,
            Long foundationId,
            String status,
            Long representanteId) {
        this(requestId, publicationId, donorId, foundationId, status, Instant.now(), representanteId, null);
    }

    public DonationRequestCreatedEvent(
            Long requestId,
            Long publicationId,
            Long donorId,
            Long foundationId,
            String status,
            Long representanteId,
            String resumen) {
        this(requestId, publicationId, donorId, foundationId, status, Instant.now(), representanteId, resumen);
    }

    public DonationRequestCreatedEvent(Long requestId, Long publicationId, Long donorId, Long foundationId, String status) {
        this(requestId, publicationId, donorId, foundationId, status, Instant.now(), null, null);
    }

    public DonationRequestCreatedEvent(Long requestId, Long publicationId, Long donorId, Long foundationId, Instant occurredOn) {
        this(requestId, publicationId, donorId, foundationId, "PENDING", occurredOn, null, null);
    }

    public DonationRequestCreatedEvent(Long requestId, Long publicationId, Long donorId, Long foundationId) {
        this(requestId, publicationId, donorId, foundationId, "PENDING", Instant.now(), null, null);
    }
}
