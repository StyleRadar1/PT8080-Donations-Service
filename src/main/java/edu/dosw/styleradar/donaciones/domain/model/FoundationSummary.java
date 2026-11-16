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
        boolean activa,
        Long representanteId
) {
    public FoundationSummary {
        categoriasAceptadas = categoriasAceptadas == null ? List.of() : List.copyOf(categoriasAceptadas);
    }

    public FoundationSummary(
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
        this(id, nombre, barrio, descripcion, direccion, telefono, email, categoriasAceptadas,
                causasApoyadas, horarioAtencion, politicasDonacion, activa, null);
    }
}
