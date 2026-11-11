package edu.dosw.styleradar.donaciones.domain.event;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Evento emitido al coordinar la fecha, franja horaria y punto de entrega para la recogida de la donación.
 */
public record PickupScheduledEvent(
        Long requestId,
        String deliveryPoint,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Instant occurredOn
) {
    public PickupScheduledEvent(Long requestId, String deliveryPoint, LocalDateTime startTime, LocalDateTime endTime) {
        this(requestId, deliveryPoint, startTime, endTime, Instant.now());
    }
}
