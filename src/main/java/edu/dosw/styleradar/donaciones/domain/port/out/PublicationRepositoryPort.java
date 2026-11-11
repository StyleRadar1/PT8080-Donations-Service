package edu.dosw.styleradar.donaciones.domain.port.out;

import java.util.List;
import java.util.Optional;

import edu.dosw.styleradar.donaciones.domain.model.PublicationSearchCriteria;
import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;

/**
 * Puerto de salida (SPI) para la persistencia y recuperación de publicaciones de segunda mano.
 */
public interface PublicationRepositoryPort {

    SecondHandPublication save(SecondHandPublication publication);

    Optional<SecondHandPublication> findById(Long id);

    List<SecondHandPublication> findByUserId(Long userId);

    List<SecondHandPublication> findByCriteria(PublicationSearchCriteria criteria);

    boolean existsById(Long id);

    void delete(Long id);
}
