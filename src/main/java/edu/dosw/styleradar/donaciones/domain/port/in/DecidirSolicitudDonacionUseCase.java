package edu.dosw.styleradar.donaciones.domain.port.in;

import edu.dosw.styleradar.donaciones.domain.model.SolicitudDonacion;

/**
 * Caso de uso (Puerto de Entrada): Decisión de aprobación o rechazo de una solicitud por parte de una fundación.
 */
public interface DecidirSolicitudDonacionUseCase {

    SolicitudDonacion decidir(Long fundacionId, Long solicitudId, boolean aprobar, String motivo);
}
