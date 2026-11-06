package edu.dosw.styleradar.donaciones.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import edu.dosw.styleradar.donaciones.domain.event.RecogidaProgramadaEvent;
import edu.dosw.styleradar.donaciones.domain.exception.RecursoNoEncontradoException;
import edu.dosw.styleradar.donaciones.domain.exception.SolicitudDonacionInvalidaException;
import edu.dosw.styleradar.donaciones.domain.model.SolicitudDonacion;
import edu.dosw.styleradar.donaciones.domain.port.in.ProgramarRecogidaCommand;
import edu.dosw.styleradar.donaciones.domain.port.in.ProgramarRecogidaDonacionUseCase;
import edu.dosw.styleradar.donaciones.domain.port.out.DonacionEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.SolicitudDonacionRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Implementación del caso de uso de coordinación de cita y franja horaria de entrega o recogida.
 */
@Service
@RequiredArgsConstructor
public class ProgramarRecogidaDonacionUseCaseImpl implements ProgramarRecogidaDonacionUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProgramarRecogidaDonacionUseCaseImpl.class);

    private final SolicitudDonacionRepositoryPort solicitudRepositoryPort;
    private final DonacionEventPublisherPort eventPublisherPort;

    @Override
    public SolicitudDonacion programarRecogida(ProgramarRecogidaCommand command) {
        log.info("Programando recogida para la solicitud ID: {} por usuario ID: {}",
                command.solicitudId(), command.usuarioId());

        SolicitudDonacion solicitud = solicitudRepositoryPort.buscarPorId(command.solicitudId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe solicitud de donación con ID: " + command.solicitudId()));

        boolean esParticipante = solicitud.getDonanteId().equals(command.usuarioId())
                || solicitud.getFundacionId().equals(command.usuarioId());
        if (!esParticipante) {
            throw new SolicitudDonacionInvalidaException("El usuario no participa en esta solicitud de donación");
        }

        solicitud.programarRecogida(command.puntoEntrega(), command.franjaInicio(), command.franjaFin());

        SolicitudDonacion guardada = solicitudRepositoryPort.guardar(solicitud);
        log.info("Recogida programada con éxito para la solicitud ID: {}", guardada.getId());

        eventPublisherPort.publicarEvento(
                new RecogidaProgramadaEvent(
                        guardada.getId(),
                        guardada.getPuntoEntrega(),
                        guardada.getFranjaInicio(),
                        guardada.getFranjaFin()
                )
        );

        return guardada;
    }
}
