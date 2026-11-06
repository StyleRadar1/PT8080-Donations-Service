package edu.dosw.styleradar.donaciones.domain.port.in;

import edu.dosw.styleradar.donaciones.domain.model.SolicitudDonacion;

/**
 * Caso de uso (Puerto de Entrada): Coordinación de la logística de entrega o recogida domiciliaria.
 */
public interface ProgramarRecogidaDonacionUseCase {

    SolicitudDonacion programarRecogida(ProgramarRecogidaCommand command);
}
