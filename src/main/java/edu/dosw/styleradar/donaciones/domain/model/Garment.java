package edu.dosw.styleradar.donaciones.domain.model;

import edu.dosw.styleradar.donaciones.domain.exception.InvalidPublicationException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad de dominio que representa una prenda de vestir asociada a una publicación.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Garment {

    private Long id;
    private String name;
    private String description;
    private GarmentType type;
    private String brand;
    private String color;
    private Style style;

    /**
     * Valida que los datos obligatorios de la prenda satisfagan las reglas de integridad de negocio.
     */
    public void validateInvariants() {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidPublicationException("El nombre de la prenda es obligatorio");
        }
        if (type == null) {
            throw new InvalidPublicationException("El tipo de prenda es obligatorio");
        }
    }
}
