package edu.dosw.styleradar.donaciones.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import edu.dosw.styleradar.donaciones.domain.event.SolicitudDonacionDecididaEvent;
import edu.dosw.styleradar.donaciones.domain.exception.RecursoNoEncontradoException;
import edu.dosw.styleradar.donaciones.domain.exception.SolicitudDonacionInvalidaException;
import edu.dosw.styleradar.donaciones.domain.model.SolicitudDonacion;
import edu.dosw.styleradar.donaciones.domain.port.in.DecidirSolicitudDonacionUseCase;
import edu.dosw.styleradar.donaciones.domain.port.out.DonacionEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.SolicitudDonacionRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Implementación del caso de uso de aprobación o rechazo de solicitudes de donación.
 */
@Service
@RequiredArgsConstructor
public class DecidirSolicitudDonacionUseCaseImpl implements DecidirSolicitudDonacionUseCase {

    private static final Logger log = LoggerFactory.getLogger(DecidirSolicitudDonacionUseCaseImpl.class);

    private final SolicitudDonacionRepositoryPort solicitudRepositoryPort;
    private final DonacionEventPublisherPort eventPublisherPort;

    @Override
    public SolicitudDonacion decidir(Long fundacionId, Long solicitudId, boolean aprobar, String motivo) {
        log.info("Fundación ID: {} procesa decisión para la solicitud ID: {}. Aprobada: {}",
                fundacionId, solicitudId, aprobar);

        SolicitudDonacion solicitud = solicitudRepositoryPort.buscarPorId(solicitudId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe solicitud de donación con ID: " + solicitudId));

        if (!solicitud.getFundacionId().equals(fundacionId)) {
            throw new SolicitudDonacionInvalidaException("La solicitud no corresponde a la fundación indicada");
        }

        if (aprobar) {
            solicitud.aprobar();
        } else {
            solicitud.rechazar(motivo);
        }

        SolicitudDonacion guardada = solicitudRepositoryPort.guardar(solicitud);
        log.info("Solicitud ID: {} actualizada con nuevo estado: {}", guardada.getId(), guardada.getEstado());

        eventPublisherPort.publicarEvento(
                new SolicitudDonacionDecididaEvent(
                        guardada.getId(),
                        guardada.getFundacionId(),
                        aprobar,
                        guardada.getMotivoRechazo()
                )
        );

        return guardada;
    }
}
