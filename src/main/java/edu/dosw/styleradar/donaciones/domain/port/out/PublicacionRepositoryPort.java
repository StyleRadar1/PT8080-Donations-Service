package edu.dosw.styleradar.donaciones.domain.port.out;

import java.util.List;
import java.util.Optional;

import edu.dosw.styleradar.donaciones.domain.model.BusquedaPublicacionesCriteria;
import edu.dosw.styleradar.donaciones.domain.model.PublicacionSegundaMano;

/**
 * Puerto de salida (SPI) para la persistencia y recuperación de publicaciones de segunda mano.
 */
public interface PublicacionRepositoryPort {

    PublicacionSegundaMano guardar(PublicacionSegundaMano publicacion);

    Optional<PublicacionSegundaMano> buscarPorId(Long id);

    List<PublicacionSegundaMano> buscarPorUsuarioId(Long usuarioId);

    List<PublicacionSegundaMano> buscarConCriterios(BusquedaPublicacionesCriteria criteria);

    boolean existePorId(Long id);

    void eliminar(Long id);
}
