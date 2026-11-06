package edu.dosw.styleradar.donaciones.domain.event;

import java.time.Instant;

/**
 * Evento emitido al confirmarse la recepción física de la prenda donada en la fundación.
 */
public record DonacionEntregadaEvent(
        Long solicitudId,
        Long publicacionId,
        Long fundacionId,
        Instant ocurridoEn
) {
    public DonacionEntregadaEvent(Long solicitudId, Long publicacionId, Long fundacionId) {
        this(solicitudId, publicacionId, fundacionId, Instant.now());
    }
}
