package edu.dosw.styleradar.donaciones.domain.port.in;

import java.util.List;

import edu.dosw.styleradar.donaciones.domain.model.PublicationSearchCriteria;
import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;

/**
 * Puerto de entrada (Caso de Uso): Consulta y búsqueda de publicaciones de segunda mano.
 */
public interface GetPublicationsUseCase {

    SecondHandPublication getById(Long id);

    List<SecondHandPublication> getByUserId(Long userId);

    List<SecondHandPublication> search(PublicationSearchCriteria criteria);
}
