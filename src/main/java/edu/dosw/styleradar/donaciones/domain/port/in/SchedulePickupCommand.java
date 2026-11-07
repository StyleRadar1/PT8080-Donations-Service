package edu.dosw.styleradar.donaciones.domain.port.in;

import java.time.LocalDateTime;

/**
 * Comando para agendar el punto y la franja horaria de recogida/entrega de una donación aprobada.
 */
public record SchedulePickupCommand(
        Long userId,
        Long requestId,
        String deliveryPoint,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
