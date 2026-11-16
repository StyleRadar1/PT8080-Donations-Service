package edu.dosw.styleradar.donaciones.domain.model;

import java.util.List;

/**
 * Modelo de dominio que resume la información institucional de una fundación aliada
 * obtenida desde el catálogo maestro de ms-tiendas.
 */
public record FoundationSummary(
        Long id,
        String nombre,
        String barrio,
        String descripcion,
        String direccion,
        String telefono,
        String email,
        List<String> categoriasAceptadas,
        String causasApoyadas,
        String horarioAtencion,
        String politicasDonacion,
        boolean activa
) {
    public FoundationSummary {
        categoriasAceptadas = categoriasAceptadas == null ? List.of() : List.copyOf(categoriasAceptadas);
    }
}
