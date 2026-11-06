package edu.dosw.styleradar.donaciones.domain.port.in;

import java.math.BigDecimal;
import java.util.List;

import edu.dosw.styleradar.donaciones.domain.model.ConservationStatus;
import edu.dosw.styleradar.donaciones.domain.model.Garment;
import edu.dosw.styleradar.donaciones.domain.model.Size;

/**
 * Comando para actualizar la información, talla, estado, precio y fotos de una publicación existente.
 */
public record EditPublicationCommand(
        Long userId,
        Long publicationId,
        Garment garment,
        Size size,
        ConservationStatus conservationStatus,
        BigDecimal price,
        List<String> photos
) {
    public EditPublicationCommand {
        photos = photos != null ? List.copyOf(photos) : List.of();
    }
}
