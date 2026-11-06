package edu.dosw.styleradar.donaciones.domain.port.out;

import java.util.List;
import java.util.Optional;

import edu.dosw.styleradar.donaciones.domain.model.EstadoSolicitudDonacion;
import edu.dosw.styleradar.donaciones.domain.model.SolicitudDonacion;

/**
 * Puerto de salida (SPI) para la persistencia y recuperación de solicitudes de donación.
 */
public interface SolicitudDonacionRepositoryPort {

    SolicitudDonacion guardar(SolicitudDonacion solicitud);

    Optional<SolicitudDonacion> buscarPorId(Long id);

    List<SolicitudDonacion> buscarPorDonanteId(Long donanteId);

    List<SolicitudDonacion> buscarPorFundacionId(Long fundacionId);

    List<SolicitudDonacion> buscarPorFundacionIdYEstado(Long fundacionId, EstadoSolicitudDonacion estado);

    boolean existeSolicitudEnCursoParaPublicacion(Long publicacionId);
}
