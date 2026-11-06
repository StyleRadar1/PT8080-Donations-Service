package edu.dosw.styleradar.donaciones.domain.event;

import java.time.Instant;

/**
 * Evento emitido cuando una prenda de segunda mano ha sido adquirida por un comprador.
 */
public record PublicacionVendidaEvent(
        Long publicacionId,
        Long usuarioId,
        Instant ocurridoEn
) {
    public PublicacionVendidaEvent(Long publicacionId, Long usuarioId) {
        this(publicacionId, usuarioId, Instant.now());
    }
}
