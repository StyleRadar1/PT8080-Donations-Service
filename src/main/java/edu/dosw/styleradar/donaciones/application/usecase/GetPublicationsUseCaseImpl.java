package edu.dosw.styleradar.donaciones.application.usecase;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.donaciones.domain.exception.InvalidPublicationException;
import edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException;
import edu.dosw.styleradar.donaciones.domain.model.PublicationSearchCriteria;
import edu.dosw.styleradar.donaciones.domain.model.PublicationStatus;
import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;
import edu.dosw.styleradar.donaciones.domain.port.in.GetPublicationsUseCase;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicationRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Implementación del caso de uso de consulta y búsqueda de publicaciones de segunda mano.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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
        PublicationSearchCriteria effectiveCriteria;
        if (criteria == null) {
            effectiveCriteria = PublicationSearchCriteria.builder()
                    .status(PublicationStatus.AVAILABLE)
                    .build();
        } else if (criteria.getStatus() == null) {
            effectiveCriteria = criteria.toBuilder()
                    .status(PublicationStatus.AVAILABLE)
                    .build();
        } else {
            effectiveCriteria = criteria;
        }
        validatePriceRange(effectiveCriteria);
        log.info("Ejecutando búsqueda de publicaciones con criterios: {}", effectiveCriteria);
        return publicationRepositoryPort.findByCriteria(effectiveCriteria);
    }

    private void validatePriceRange(PublicationSearchCriteria criteria) {
        BigDecimal minPrice = criteria.getMinPrice();
        BigDecimal maxPrice = criteria.getMaxPrice();
        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidPublicationException("El precio mínimo no puede ser negativo");
        }
        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidPublicationException("El precio máximo no puede ser negativo");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new InvalidPublicationException("El precio mínimo no puede ser superior al precio máximo");
        }
    }
}
