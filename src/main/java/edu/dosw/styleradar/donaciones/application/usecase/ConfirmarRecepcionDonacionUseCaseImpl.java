package edu.dosw.styleradar.donaciones.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import edu.dosw.styleradar.donaciones.domain.event.DonacionEntregadaEvent;
import edu.dosw.styleradar.donaciones.domain.exception.RecursoNoEncontradoException;
import edu.dosw.styleradar.donaciones.domain.exception.SolicitudDonacionInvalidaException;
import edu.dosw.styleradar.donaciones.domain.model.PublicacionSegundaMano;
import edu.dosw.styleradar.donaciones.domain.model.SolicitudDonacion;
import edu.dosw.styleradar.donaciones.domain.port.in.ConfirmarRecepcionDonacionUseCase;
import edu.dosw.styleradar.donaciones.domain.port.out.DonacionEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicacionRepositoryPort;
import edu.dosw.styleradar.donaciones.domain.port.out.SolicitudDonacionRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Implementación del caso de uso de confirmación de recepción de prendas donadas.
 */
@Service
@RequiredArgsConstructor
public class ConfirmarRecepcionDonacionUseCaseImpl implements ConfirmarRecepcionDonacionUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConfirmarRecepcionDonacionUseCaseImpl.class);

    private final SolicitudDonacionRepositoryPort solicitudRepositoryPort;
    private final PublicacionRepositoryPort publicacionRepositoryPort;
    private final DonacionEventPublisherPort eventPublisherPort;

    @Override
    public SolicitudDonacion confirmarRecepcion(Long fundacionId, Long solicitudId) {
        log.info("Fundación ID: {} confirma recepción de la donación para la solicitud ID: {}",
                fundacionId, solicitudId);

        SolicitudDonacion solicitud = solicitudRepositoryPort.buscarPorId(solicitudId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe solicitud de donación con ID: " + solicitudId));

        if (!solicitud.getFundacionId().equals(fundacionId)) {
            throw new SolicitudDonacionInvalidaException("La solicitud no corresponde a la fundación indicada");
        }

        solicitud.confirmarEntrega();

        publicacionRepositoryPort.buscarPorId(solicitud.getPublicacionId()).ifPresent(publicacion -> {
            publicacion.marcarComoDonada();
            publicacionRepositoryPort.guardar(publicacion);
            log.info("Publicación ID: {} actualizada a estado DONADA", publicacion.getId());
        });

        SolicitudDonacion guardada = solicitudRepositoryPort.guardar(solicitud);
        log.info("Solicitud ID: {} confirmada y marcada como ENTREGADA", guardada.getId());

        eventPublisherPort.publicarEvento(
                new DonacionEntregadaEvent(
                        guardada.getId(),
                        guardada.getPublicacionId(),
                        guardada.getFundacionId()
                )
        );

        return guardada;
    }
}
