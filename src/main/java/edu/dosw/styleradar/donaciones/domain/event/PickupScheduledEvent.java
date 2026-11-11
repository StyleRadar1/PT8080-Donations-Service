package edu.dosw.styleradar.donaciones.domain.event;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Evento emitido al coordinar la fecha, franja horaria y punto de entrega para la recogida de la donación.
 */
public record PickupScheduledEvent(
        Long requestId,
        Long donorId,
        String deliveryPoint,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Instant occurredOn
) {
    public PickupScheduledEvent(Long requestId, String deliveryPoint, LocalDateTime startTime, LocalDateTime endTime) {
        this(requestId, null, deliveryPoint, startTime, endTime, Instant.now());
    }

    public PickupScheduledEvent(Long requestId, Long donorId, String deliveryPoint, LocalDateTime startTime, LocalDateTime endTime) {
        this(requestId, donorId, deliveryPoint, startTime, endTime, Instant.now());
    }
}
