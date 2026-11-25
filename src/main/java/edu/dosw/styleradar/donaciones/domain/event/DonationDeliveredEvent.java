package edu.dosw.styleradar.donaciones.domain.event;

import java.time.Instant;

/**
 * Evento emitido cuando la fundación confirma la recepción física y entrega de la prenda donada.
 * Incluye {@code eventId} estable para trazabilidad e idempotencia desde el Outbox transaccional.
 */
public record DonationDeliveredEvent(
        Long requestId,
        Long publicationId,
        Long foundationId,
        Long donorId,
        Instant occurredOn,
        String eventId
) {
    public DonationDeliveredEvent(Long requestId, Long publicationId, Long foundationId) {
        this(requestId, publicationId, foundationId, null, Instant.now(), null);
    }

    public DonationDeliveredEvent(Long requestId, Long publicationId, Long foundationId, Long donorId) {
        this(requestId, publicationId, foundationId, donorId, Instant.now(), null);
    }

    public DonationDeliveredEvent(Long requestId, Long publicationId, Long foundationId, Long donorId, Instant occurredOn) {
        this(requestId, publicationId, foundationId, donorId, occurredOn, null);
    }
}
