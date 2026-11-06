package edu.dosw.styleradar.donaciones.application.usecase;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import edu.dosw.styleradar.donaciones.domain.exception.RecursoNoEncontradoException;
import edu.dosw.styleradar.donaciones.domain.model.BusquedaPublicacionesCriteria;
import edu.dosw.styleradar.donaciones.domain.model.PublicacionSegundaMano;
import edu.dosw.styleradar.donaciones.domain.port.in.ConsultarPublicacionesUseCase;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicacionRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Implementación del caso de uso de consulta y búsqueda de publicaciones de segunda mano.
 */
@Service
@RequiredArgsConstructor
public class ConsultarPublicacionesUseCaseImpl implements ConsultarPublicacionesUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConsultarPublicacionesUseCaseImpl.class);

    private final PublicacionRepositoryPort publicacionRepositoryPort;

    @Override
    public PublicacionSegundaMano obtenerPorId(Long id) {
        log.info("Consultando publicación de segunda mano con ID: {}", id);
        return publicacionRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe publicación de segunda mano con ID: " + id));
    }

    @Override
    public List<PublicacionSegundaMano> obtenerPorUsuario(Long usuarioId) {
        log.info("Consultando publicaciones asociadas al usuario ID: {}", usuarioId);
        return publicacionRepositoryPort.buscarPorUsuarioId(usuarioId);
    }

    @Override
    public List<PublicacionSegundaMano> buscar(BusquedaPublicacionesCriteria criteria) {
        log.info("Ejecutando búsqueda de publicaciones con criterios: {}", criteria);
        return publicacionRepositoryPort.buscarConCriterios(criteria);
    }
}
