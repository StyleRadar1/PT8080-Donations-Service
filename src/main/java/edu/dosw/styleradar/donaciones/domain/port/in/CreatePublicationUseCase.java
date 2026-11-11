package edu.dosw.styleradar.donaciones.domain.port.in;

import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;

/**
 * Puerto de entrada (Caso de Uso): Creación de publicaciones de prendas usadas.
 */
public interface CreatePublicationUseCase {

    SecondHandPublication create(CreatePublicationCommand command);
}
