package edu.dosw.styleradar.donaciones.domain.port.in;

/**
 * Comando para postular una prenda publicada al proceso de donación hacia una fundación.
 */
public record CrearSolicitudDonacionCommand(
        Long donanteId,
        Long publicacionId,
        Long fundacionId,
        String mensaje
) {
}
