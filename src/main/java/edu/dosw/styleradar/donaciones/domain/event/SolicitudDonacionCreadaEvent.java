package edu.dosw.styleradar.donaciones.domain.event;

import java.time.Instant;

/**
 * Evento emitido al enviarse una nueva solicitud de donación hacia una fundación benéfica.
 */
public record SolicitudDonacionCreadaEvent(
        Long solicitudId,
        Long publicacionId,
        Long donanteId,
        Long fundacionId,
        Instant ocurridoEn
) {
    public SolicitudDonacionCreadaEvent(Long solicitudId, Long publicacionId, Long donanteId, Long fundacionId) {
        this(solicitudId, publicacionId, donanteId, fundacionId, Instant.now());
    }
}
