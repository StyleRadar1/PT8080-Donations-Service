package edu.dosw.styleradar.donaciones.domain.port.in;

import java.util.List;

import edu.dosw.styleradar.donaciones.domain.model.BusquedaPublicacionesCriteria;
import edu.dosw.styleradar.donaciones.domain.model.PublicacionSegundaMano;

/**
 * Caso de uso (Puerto de Entrada): Búsqueda y consulta de publicaciones de segunda mano.
 */
public interface ConsultarPublicacionesUseCase {

    PublicacionSegundaMano obtenerPorId(Long id);

    List<PublicacionSegundaMano> obtenerPorUsuario(Long usuarioId);

    List<PublicacionSegundaMano> buscar(BusquedaPublicacionesCriteria criteria);
}
