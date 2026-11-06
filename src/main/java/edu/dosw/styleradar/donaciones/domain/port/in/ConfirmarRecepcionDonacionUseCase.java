package edu.dosw.styleradar.donaciones.domain.port.in;

import edu.dosw.styleradar.donaciones.domain.model.SolicitudDonacion;

/**
 * Caso de uso (Puerto de Entrada): Confirmación de recepción física de las prendas donadas por la fundación.
 */
public interface ConfirmarRecepcionDonacionUseCase {

    SolicitudDonacion confirmarRecepcion(Long fundacionId, Long solicitudId);
}
