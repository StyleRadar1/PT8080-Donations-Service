package edu.dosw.styleradar.donaciones.domain.port.in;

import java.math.BigDecimal;
import java.util.List;

import edu.dosw.styleradar.donaciones.domain.model.ConservationStatus;
import edu.dosw.styleradar.donaciones.domain.model.Garment;
import edu.dosw.styleradar.donaciones.domain.model.LocalidadBogota;
import edu.dosw.styleradar.donaciones.domain.model.Size;

/**
 * Comando para originar una nueva publicación en el módulo de Economía Circular (Segunda Mano o Donación).
 * Incluye discriminador 'esDonacion' y la localidad oficial de Bogotá D.C.
 */
public record CreatePublicationCommand(
        Long userId,
        Garment garment,
        Size size,
        ConservationStatus conservationStatus,
        BigDecimal price,
        boolean esDonacion,
        LocalidadBogota localidad,
        List<String> photos
) {
    public CreatePublicationCommand {
        photos = photos != null ? List.copyOf(photos) : List.of();
        if (esDonacion) {
            price = BigDecimal.ZERO;
        }
        if (localidad == null) {
            localidad = LocalidadBogota.CHAPINERO;
        }
    }

    /**
     * Constructor de sobrecarga para compatibilidad hacia atrás.
     */
    public CreatePublicationCommand(
            Long userId,
            Garment garment,
            Size size,
            ConservationStatus conservationStatus,
            BigDecimal price,
            List<String> photos
    ) {
        this(userId, garment, size, conservationStatus, price, false, LocalidadBogota.CHAPINERO, photos);
    }
}
