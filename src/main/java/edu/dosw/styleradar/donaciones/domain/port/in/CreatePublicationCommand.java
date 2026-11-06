package edu.dosw.styleradar.donaciones.domain.port.in;

import java.math.BigDecimal;
import java.util.List;

import edu.dosw.styleradar.donaciones.domain.model.ConservationStatus;
import edu.dosw.styleradar.donaciones.domain.model.Garment;
import edu.dosw.styleradar.donaciones.domain.model.Size;

/**
 * Comando para originar una nueva publicación de prenda usada en la plataforma.
 */
public record CreatePublicationCommand(
        Long userId,
        Garment garment,
        Size size,
        ConservationStatus conservationStatus,
        BigDecimal price,
        List<String> photos
) {
    public CreatePublicationCommand {
        photos = photos != null ? List.copyOf(photos) : List.of();
    }
}
