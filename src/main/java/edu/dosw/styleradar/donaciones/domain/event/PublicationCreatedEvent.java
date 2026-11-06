package edu.dosw.styleradar.donaciones.domain.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Evento emitido al crearse exitosamente una nueva publicación de prenda usada.
 */
public record PublicationCreatedEvent(
        Long publicationId,
        Long userId,
        BigDecimal price,
        Instant occurredOn
) {
    public PublicationCreatedEvent(Long publicationId, Long userId, BigDecimal price) {
        this(publicationId, userId, price, Instant.now());
    }
}
