package edu.dosw.styleradar.donaciones.domain.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de valor que encapsula los filtros de búsqueda para las publicaciones de economía circular.
 * Permite filtrar por discriminador 'esDonacion' y por localidad oficial de Bogotá D.C.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicationSearchCriteria {

    private String searchTerm;
    private GarmentType type;
    private String color;
    private Size size;
    private ConservationStatus conservationStatus;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String brand;
    private Style style;
    private PublicationStatus status;
    private Boolean esDonacion;
    private LocalidadBogota localidad;
}
