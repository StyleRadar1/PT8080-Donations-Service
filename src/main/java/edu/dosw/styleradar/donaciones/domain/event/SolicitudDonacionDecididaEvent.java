package edu.dosw.styleradar.donaciones.domain.event;

import java.time.Instant;

/**
 * Evento emitido cuando una fundación aprueba o rechaza una solicitud de donación.
 */
public record SolicitudDonacionDecididaEvent(
        Long solicitudId,
        Long fundacionId,
        boolean aprobada,
        String motivoRechazo,
        Instant ocurridoEn
) {
    public SolicitudDonacionDecididaEvent(Long solicitudId, Long fundacionId, boolean aprobada, String motivoRechazo) {
        this(solicitudId, fundacionId, aprobada, motivoRechazo, Instant.now());
    }
}
