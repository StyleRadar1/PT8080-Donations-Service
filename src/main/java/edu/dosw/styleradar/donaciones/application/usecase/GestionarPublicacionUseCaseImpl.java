package edu.dosw.styleradar.donaciones.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import edu.dosw.styleradar.donaciones.domain.event.PublicacionVendidaEvent;
import edu.dosw.styleradar.donaciones.domain.exception.PublicacionInvalidaException;
import edu.dosw.styleradar.donaciones.domain.exception.RecursoNoEncontradoException;
import edu.dosw.styleradar.donaciones.domain.model.PublicacionSegundaMano;
import edu.dosw.styleradar.donaciones.domain.port.in.EditarPublicacionCommand;
import edu.dosw.styleradar.donaciones.domain.port.in.GestionarPublicacionUseCase;
import edu.dosw.styleradar.donaciones.domain.port.out.DonacionEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicacionRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Implementación del caso de uso de gestión (edición, retiro, venta y paso a donación) de publicaciones.
 */
@Service
@RequiredArgsConstructor
public class GestionarPublicacionUseCaseImpl implements GestionarPublicacionUseCase {

    private static final Logger log = LoggerFactory.getLogger(GestionarPublicacionUseCaseImpl.class);

    private final PublicacionRepositoryPort publicacionRepositoryPort;
    private final DonacionEventPublisherPort eventPublisherPort;

    @Override
    public PublicacionSegundaMano editar(EditarPublicacionCommand command) {
        log.info("Editando publicación ID: {} por usuario ID: {}", command.publicacionId(), command.usuarioId());
        PublicacionSegundaMano publicacion = obtenerYValidarPropietario(command.publicacionId(), command.usuarioId());

        publicacion.editar(
                command.prenda(),
                command.talla(),
                command.estadoConservacion(),
                command.precio(),
                command.fotos()
        );

        PublicacionSegundaMano guardada = publicacionRepositoryPort.guardar(publicacion);
        log.info("Publicación ID: {} actualizada con éxito", guardada.getId());
        return guardada;
    }

    @Override
    public PublicacionSegundaMano marcarComoVendida(Long usuarioId, Long publicacionId) {
        log.info("Marcando como vendida publicación ID: {} por usuario ID: {}", publicacionId, usuarioId);
        PublicacionSegundaMano publicacion = obtenerYValidarPropietario(publicacionId, usuarioId);

        publicacion.marcarComoVendida();
        PublicacionSegundaMano guardada = publicacionRepositoryPort.guardar(publicacion);

        eventPublisherPort.publicarEvento(new PublicacionVendidaEvent(guardada.getId(), guardada.getUsuarioId()));
        log.info("Publicación ID: {} marcada como VENDIDA exitosamente", guardada.getId());
        return guardada;
    }

    @Override
    public void retirar(Long usuarioId, Long publicacionId) {
        log.info("Retirando publicación ID: {} por usuario ID: {}", publicacionId, usuarioId);
        PublicacionSegundaMano publicacion = obtenerYValidarPropietario(publicacionId, usuarioId);

        publicacion.retirar();
        publicacionRepositoryPort.guardar(publicacion);
        log.info("Publicación ID: {} retirada exitosamente", publicacionId);
    }

    @Override
    public PublicacionSegundaMano destinarADonacion(Long usuarioId, Long publicacionId) {
        log.info("Destinando publicación ID: {} hacia canal de donación", publicacionId);
        PublicacionSegundaMano publicacion = obtenerYValidarPropietario(publicacionId, usuarioId);

        publicacion.destinarADonacion();
        PublicacionSegundaMano guardada = publicacionRepositoryPort.guardar(publicacion);
        log.info("Publicación ID: {} en estado EN_DONACION", guardada.getId());
        return guardada;
    }

    private PublicacionSegundaMano obtenerYValidarPropietario(Long publicacionId, Long usuarioId) {
        PublicacionSegundaMano publicacion = publicacionRepositoryPort.buscarPorId(publicacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe publicación con ID: " + publicacionId));

        if (!publicacion.getUsuarioId().equals(usuarioId)) {
            throw new PublicacionInvalidaException("La publicación no pertenece al usuario especificado");
        }
        return publicacion;
    }
}
