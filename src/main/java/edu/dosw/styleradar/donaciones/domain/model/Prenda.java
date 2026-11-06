package edu.dosw.styleradar.donaciones.domain.model;

import edu.dosw.styleradar.donaciones.domain.exception.PublicacionInvalidaException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad del dominio que representa una prenda de vestir asociada a una publicación.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Prenda {

    private Long id;
    private String nombre;
    private String descripcion;
    private TipoPrenda tipo;
    private String marca;
    private String color;
    private Estilo estilo;

    /**
     * Valida que los datos obligatorios de la prenda satisfagan las reglas de integridad.
     */
    public void validarInvariantes() {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new PublicacionInvalidaException("El nombre de la prenda es obligatorio");
        }
        if (tipo == null) {
            throw new PublicacionInvalidaException("El tipo de prenda es obligatorio");
        }
    }
}
