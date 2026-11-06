package edu.dosw.styleradar.donaciones.domain.port.in;

import java.util.List;

import edu.dosw.styleradar.donaciones.domain.model.EstadoSolicitudDonacion;
import edu.dosw.styleradar.donaciones.domain.model.SolicitudDonacion;

/**
 * Caso de uso (Puerto de Entrada): Creación y consulta de solicitudes de donación para donantes y fundaciones.
 */
public interface CrearSolicitudDonacionUseCase {

    SolicitudDonacion crear(CrearSolicitudDonacionCommand command);

    List<SolicitudDonacion> consultarPorDonante(Long donanteId);

    List<SolicitudDonacion> consultarPorFundacion(Long fundacionId, EstadoSolicitudDonacion estado);

    SolicitudDonacion obtenerPorId(Long id);
}
