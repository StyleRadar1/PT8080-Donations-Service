package edu.dosw.styleradar.donaciones.domain.port.in;

import java.util.List;

import edu.dosw.styleradar.donaciones.domain.model.EstadoConservacion;
import edu.dosw.styleradar.donaciones.domain.model.Prenda;
import edu.dosw.styleradar.donaciones.domain.model.Talla;

/**
 * Comando para la edición de los datos y fotografías de una publicación existente.
 */
public record EditarPublicacionCommand(
        Long usuarioId,
        Long publicacionId,
        Prenda prenda,
        Talla talla,
        EstadoConservacion estadoConservacion,
        Double precio,
        List<String> fotos
) {
    public EditarPublicacionCommand {
        fotos = fotos != null ? List.copyOf(fotos) : List.of();
    }
}
