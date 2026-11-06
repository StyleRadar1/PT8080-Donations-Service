package edu.dosw.styleradar.donaciones.domain.port.in;

import edu.dosw.styleradar.donaciones.domain.model.PublicacionSegundaMano;

/**
 * Caso de uso (Puerto de Entrada): Creación y publicación de prendas de segunda mano.
 */
public interface CrearPublicacionUseCase {

    PublicacionSegundaMano crear(CrearPublicacionCommand command);
}
