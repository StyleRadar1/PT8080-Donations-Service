package edu.dosw.styleradar.donaciones.domain.port.in;

import java.math.BigDecimal;
import java.util.List;

import edu.dosw.styleradar.donaciones.domain.model.ConservationStatus;
import edu.dosw.styleradar.donaciones.domain.model.Garment;
import edu.dosw.styleradar.donaciones.domain.model.LocalidadBogota;
import edu.dosw.styleradar.donaciones.domain.model.Size;

/**
 * Comando para actualizar la información, talla, estado, precio, localidad y fotos de una publicación existente.
 */
public record EditPublicationCommand(
        Long userId,
        Long publicationId,
        Garment garment,
        Size size,
        ConservationStatus conservationStatus,
        BigDecimal price,
        LocalidadBogota localidad,
        List<String> photos
) {
    public EditPublicationCommand {
        photos = photos != null ? List.copyOf(photos) : List.of();
    }

    /**
     * Constructor de sobrecarga para compatibilidad hacia atrás.
     */
    public EditPublicationCommand(
            Long userId,
            Long publicationId,
            Garment garment,
            Size size,
            ConservationStatus conservationStatus,
            BigDecimal price,
            List<String> photos
    ) {
        this(userId, publicationId, garment, size, conservationStatus, price, null, photos);
    }
}
