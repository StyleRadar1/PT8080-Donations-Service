package edu.dosw.styleradar.donaciones.domain.port.in;

import java.time.Instant;

/**
 * Comando para agendar la franja horaria y dirección o punto de entrega de una donación aprobada.
 */
public record ProgramarRecogidaCommand(
        Long usuarioId,
        Long solicitudId,
        String puntoEntrega,
        Instant franjaInicio,
        Instant franjaFin
) {
}
