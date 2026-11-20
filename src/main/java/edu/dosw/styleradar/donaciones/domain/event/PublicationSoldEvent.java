package edu.dosw.styleradar.donaciones.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Evento emitido al concretarse la venta de una prenda de segunda mano en la plataforma.
 */
public record PublicationSoldEvent(
        Long publicationId,
        Long userId,
        Long compradorId,
        List<Long> interesadosIds,
        Instant occurredOn,
        String nombrePrenda
) {
    public PublicationSoldEvent {
        interesadosIds = sanitizeInteresados(interesadosIds, userId, compradorId);
    }

    public PublicationSoldEvent(
            Long publicationId,
            Long userId,
            Long compradorId,
            List<Long> interesadosIds,
            Instant occurredOn) {
        this(publicationId, userId, compradorId, interesadosIds, occurredOn, null);
    }

    public PublicationSoldEvent(Long publicationId, Long userId) {
        this(publicationId, userId, null, List.of(), Instant.now(), null);
    }

    public PublicationSoldEvent(Long publicationId, Long userId, Long compradorId) {
        this(publicationId, userId, compradorId, List.of(), Instant.now(), null);
    }

    public PublicationSoldEvent(Long publicationId, Long userId, Long compradorId, List<Long> interesadosIds) {
        this(publicationId, userId, compradorId, interesadosIds, Instant.now(), null);
    }

    public PublicationSoldEvent(
            Long publicationId,
            Long userId,
            Long compradorId,
            List<Long> interesadosIds,
            String nombrePrenda) {
        this(publicationId, userId, compradorId, interesadosIds, Instant.now(), nombrePrenda);
    }

    public PublicationSoldEvent(Long publicationId, Long userId, Instant occurredOn) {
        this(publicationId, userId, null, List.of(), occurredOn, null);
    }

    private static List<Long> sanitizeInteresados(List<Long> rawInteresados, Long sellerId, Long buyerId) {
        if (rawInteresados == null || rawInteresados.isEmpty()) {
            return List.of();
        }
        return rawInteresados.stream()
                .filter(Objects::nonNull)
                .filter(id -> !id.equals(sellerId) && !id.equals(buyerId))
                .distinct()
                .toList();
    }
}
