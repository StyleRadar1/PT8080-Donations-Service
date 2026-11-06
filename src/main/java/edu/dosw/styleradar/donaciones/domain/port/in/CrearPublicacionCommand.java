package edu.dosw.styleradar.donaciones.domain.port.in;

import java.util.List;

import edu.dosw.styleradar.donaciones.domain.model.EstadoConservacion;
import edu.dosw.styleradar.donaciones.domain.model.Prenda;
import edu.dosw.styleradar.donaciones.domain.model.Talla;

/**
 * Comando para la creación de una nueva publicación de segunda mano.
 */
public record CrearPublicacionCommand(
        Long usuarioId,
        Prenda prenda,
        Talla talla,
        EstadoConservacion estadoConservacion,
        Double precio,
        List<String> fotos
) {
    public CrearPublicacionCommand {
        fotos = fotos != null ? List.copyOf(fotos) : List.of();
    }
}
