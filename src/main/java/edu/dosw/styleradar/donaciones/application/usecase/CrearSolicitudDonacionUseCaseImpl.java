package edu.dosw.styleradar.donaciones.application.usecase;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import edu.dosw.styleradar.donaciones.domain.event.SolicitudDonacionCreadaEvent;
import edu.dosw.styleradar.donaciones.domain.exception.ConflictoDominioException;
import edu.dosw.styleradar.donaciones.domain.exception.PublicacionInvalidaException;
import edu.dosw.styleradar.donaciones.domain.exception.RecursoNoEncontradoException;
import edu.dosw.styleradar.donaciones.domain.exception.SolicitudDonacionInvalidaException;
import edu.dosw.styleradar.donaciones.domain.model.EstadoPublicacion;
import edu.dosw.styleradar.donaciones.domain.model.EstadoSolicitudDonacion;
import edu.dosw.styleradar.donaciones.domain.model.PublicacionSegundaMano;
import edu.dosw.styleradar.donaciones.domain.model.SolicitudDonacion;
import edu.dosw.styleradar.donaciones.domain.port.in.CrearSolicitudDonacionCommand;
import edu.dosw.styleradar.donaciones.domain.port.in.CrearSolicitudDonacionUseCase;
import edu.dosw.styleradar.donaciones.domain.port.out.DonacionEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicacionRepositoryPort;
import edu.dosw.styleradar.donaciones.domain.port.out.SolicitudDonacionRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Implementación del caso de uso de radicación y consulta de solicitudes de donación.
 */
@Service
@RequiredArgsConstructor
public class CrearSolicitudDonacionUseCaseImpl implements CrearSolicitudDonacionUseCase {

    private static final Logger log = LoggerFactory.getLogger(CrearSolicitudDonacionUseCaseImpl.class);

    private final SolicitudDonacionRepositoryPort solicitudRepositoryPort;
    private final PublicacionRepositoryPort publicacionRepositoryPort;
    private final DonacionEventPublisherPort eventPublisherPort;

    @Override
    public SolicitudDonacion crear(CrearSolicitudDonacionCommand command) {
        log.info("Creando solicitud de donación del donante: {} para la fundación: {}",
                command.donanteId(), command.fundacionId());

        PublicacionSegundaMano publicacion = publicacionRepositoryPort.buscarPorId(command.publicacionId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe publicación de segunda mano con ID: " + command.publicacionId()));

        if (!publicacion.getUsuarioId().equals(command.donanteId())) {
            throw new PublicacionInvalidaException("La publicación no pertenece al usuario donante");
        }

        if (publicacion.getEstado() != EstadoPublicacion.EN_DONACION) {
            throw new SolicitudDonacionInvalidaException(
                    "La prenda debe estar marcada para donación antes de enviar la solicitud. Estado actual: "
                            + publicacion.getEstado());
        }

        if (solicitudRepositoryPort.existeSolicitudEnCursoParaPublicacion(command.publicacionId())) {
            throw new ConflictoDominioException("La prenda ya tiene una solicitud de donación en curso");
        }

        SolicitudDonacion solicitud = SolicitudDonacion.builder()
                .publicacionId(command.publicacionId())
                .donanteId(command.donanteId())
                .fundacionId(command.fundacionId())
                .mensaje(command.mensaje())
                .build();

        solicitud.iniciarSolicitud();
        SolicitudDonacion guardada = solicitudRepositoryPort.guardar(solicitud);

        log.info("Solicitud de donación radicada exitosamente con ID: {}", guardada.getId());

        eventPublisherPort.publicarEvento(
                new SolicitudDonacionCreadaEvent(
                        guardada.getId(),
                        guardada.getPublicacionId(),
                        guardada.getDonanteId(),
                        guardada.getFundacionId()
                )
        );

        return guardada;
    }

    @Override
    public List<SolicitudDonacion> consultarPorDonante(Long donanteId) {
        log.info("Consultando historial de donaciones del donante ID: {}", donanteId);
        return solicitudRepositoryPort.buscarPorDonanteId(donanteId);
    }

    @Override
    public List<SolicitudDonacion> consultarPorFundacion(Long fundacionId, EstadoSolicitudDonacion estado) {
        log.info("Consultando solicitudes para la fundación ID: {} con filtro de estado: {}", fundacionId, estado);
        if (estado == null) {
            return solicitudRepositoryPort.buscarPorFundacionId(fundacionId);
        }
        return solicitudRepositoryPort.buscarPorFundacionIdYEstado(fundacionId, estado);
    }

    @Override
    public SolicitudDonacion obtenerPorId(Long id) {
        log.info("Consultando solicitud de donación con ID: {}", id);
        return solicitudRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe solicitud de donación con ID: " + id));
    }
}
