package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.external.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO externo que representa la respuesta de FundacionResponseDTO expuesta por ms-tiendas.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TiendasFoundationResponseDTO(
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
        Boolean activa,
        Long representanteId
) {
}
