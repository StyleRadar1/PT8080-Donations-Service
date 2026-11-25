package edu.dosw.styleradar.donaciones.domain.event;

import java.time.Instant;

/**
 * Evento de dominio independiente emitido para {@code ms-estadisticas} ({@code styleradar.donaciones.completada})
 * cuando una fundación confirma la recepción física de una donación.
 */
public record DonationDeliveredStatisticsEvent(
        Long requestId,
        Long foundationId,
        Instant occurredOn,
        String eventId
) {
    public DonationDeliveredStatisticsEvent(Long requestId, Long foundationId, Instant occurredOn) {
        this(requestId, foundationId, occurredOn, null);
    }
}
