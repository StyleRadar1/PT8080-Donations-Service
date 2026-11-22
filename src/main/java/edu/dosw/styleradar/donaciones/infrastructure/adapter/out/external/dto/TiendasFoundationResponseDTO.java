package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.external.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO externo que representa la respuesta de FundacionResponseDTO expuesta por ms-tiendas.
 * Soporta nombres de campo en inglés (contrato vigente en QA/PROD) y español (retrocompatibilidad).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TiendasFoundationResponseDTO(
        Long id,
        @JsonAlias("name") String nombre,
        @JsonAlias("neighborhood") String barrio,
        @JsonAlias("description") String descripcion,
        @JsonAlias("address") String direccion,
        @JsonAlias("phone") String telefono,
        String email,
        @JsonAlias("acceptedCategories") List<String> categoriasAceptadas,
        @JsonAlias("supportedCauses") String causasApoyadas,
        @JsonAlias("openingHours") String horarioAtencion,
        @JsonAlias("donationPolicies") String politicasDonacion,
        @JsonAlias("active") Boolean activa,
        @JsonAlias("representativeId") Long representanteId
) {
}
