package edu.dosw.styleradar.donaciones.application.usecase;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException;
import edu.dosw.styleradar.donaciones.domain.model.PublicationSearchCriteria;
import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;
import edu.dosw.styleradar.donaciones.domain.port.in.GetPublicationsUseCase;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicationRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Implementación del caso de uso de consulta y búsqueda de publicaciones de segunda mano.
 */
@Service
@RequiredArgsConstructor
public class GetPublicationsUseCaseImpl implements GetPublicationsUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetPublicationsUseCaseImpl.class);

    private final PublicationRepositoryPort publicationRepositoryPort;

    @Override
    public SecondHandPublication getById(Long id) {
        log.info("Consultando publicación de segunda mano con ID: {}", id);
        return publicationRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe publicación de segunda mano con ID: " + id));
    }

    @Override
    public List<SecondHandPublication> getByUserId(Long userId) {
        log.info("Consultando publicaciones asociadas al usuario ID: {}", userId);
        return publicationRepositoryPort.findByUserId(userId);
    }

    @Override
    public List<SecondHandPublication> search(PublicationSearchCriteria criteria) {
        log.info("Ejecutando búsqueda de publicaciones con criterios: {}", criteria);
        return publicationRepositoryPort.findByCriteria(criteria);
    }
}
