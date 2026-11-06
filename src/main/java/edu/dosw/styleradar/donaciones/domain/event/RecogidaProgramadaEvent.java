package edu.dosw.styleradar.donaciones.domain.event;

import java.time.Instant;

/**
 * Evento emitido al coordinar la fecha y franja logística para la recogida/entrega de una donación.
 */
public record RecogidaProgramadaEvent(
        Long solicitudId,
        String puntoEntrega,
        Instant franjaInicio,
        Instant franjaFin,
        Instant ocurridoEn
) {
    public RecogidaProgramadaEvent(Long solicitudId, String puntoEntrega, Instant franjaInicio, Instant franjaFin) {
        this(solicitudId, puntoEntrega, franjaInicio, franjaFin, Instant.now());
    }
}
