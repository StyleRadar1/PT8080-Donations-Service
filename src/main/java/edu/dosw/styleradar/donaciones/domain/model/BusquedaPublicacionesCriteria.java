package edu.dosw.styleradar.donaciones.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de valor que encapsula los criterios de filtrado y búsqueda de publicaciones de segunda mano.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusquedaPublicacionesCriteria {

    private String terminoBusqueda;
    private TipoPrenda tipo;
    private String color;
    private Talla talla;
    private EstadoConservacion estadoConservacion;
    private Double precioMin;
    private Double precioMax;
    private String marca;
    private Estilo estilo;
    private EstadoPublicacion estado;
}
