package edu.dosw.styleradar.donaciones.domain.event;

import java.time.Instant;

/**
 * Evento emitido al crearse una nueva publicación de segunda mano.
 */
public record PublicacionCreadaEvent(
        Long publicacionId,
        Long usuarioId,
        Double precio,
        Instant ocurridoEn
) {
    public PublicacionCreadaEvent(Long publicacionId, Long usuarioId, Double precio) {
        this(publicacionId, usuarioId, precio, Instant.now());
    }
}
