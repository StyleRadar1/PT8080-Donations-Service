package edu.dosw.styleradar.donaciones.domain.event;

import java.time.Instant;

/**
 * Evento emitido al concretarse la venta de una prenda de segunda mano en la plataforma.
 */
public record PublicationSoldEvent(
        Long publicationId,
        Long userId,
        Instant occurredOn
) {
    public PublicationSoldEvent(Long publicationId, Long userId) {
        this(publicationId, userId, Instant.now());
    }
}
