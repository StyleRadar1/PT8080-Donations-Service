package edu.dosw.styleradar.donaciones.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import edu.dosw.styleradar.donaciones.domain.event.PublicacionCreadaEvent;
import edu.dosw.styleradar.donaciones.domain.model.PublicacionSegundaMano;
import edu.dosw.styleradar.donaciones.domain.port.in.CrearPublicacionCommand;
import edu.dosw.styleradar.donaciones.domain.port.in.CrearPublicacionUseCase;
import edu.dosw.styleradar.donaciones.domain.port.out.DonacionEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicacionRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Implementación del caso de uso de creación de publicaciones de prendas usadas.
 */
@Service
@RequiredArgsConstructor
public class CrearPublicacionUseCaseImpl implements CrearPublicacionUseCase {

    private static final Logger log = LoggerFactory.getLogger(CrearPublicacionUseCaseImpl.class);

    private final PublicacionRepositoryPort publicacionRepositoryPort;
    private final DonacionEventPublisherPort eventPublisherPort;

    @Override
    public PublicacionSegundaMano crear(CrearPublicacionCommand command) {
        log.info("Iniciando creación de publicación de segunda mano para el usuario: {}", command.usuarioId());

        PublicacionSegundaMano publicacion = PublicacionSegundaMano.builder()
                .usuarioId(command.usuarioId())
                .prenda(command.prenda())
                .talla(command.talla())
                .estadoConservacion(command.estadoConservacion())
                .precio(command.precio())
                .fotos(command.fotos())
                .build();

        publicacion.publicar();

        PublicacionSegundaMano guardada = publicacionRepositoryPort.guardar(publicacion);
        log.info("Publicación de segunda mano creada exitosamente con ID: {}", guardada.getId());

        eventPublisherPort.publicarEvento(
                new PublicacionCreadaEvent(guardada.getId(), guardada.getUsuarioId(), guardada.getPrecio())
        );

        return guardada;
    }
}
