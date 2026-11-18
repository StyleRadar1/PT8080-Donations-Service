package edu.dosw.styleradar.donaciones.application.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de respuesta para poblar el selector desplegable de fundaciones aliadas activas (SR-RF-44).
 */
@Schema(description = "Public summary information of an active partner foundation eligible to receive donations")
public record FoundationResponseDTO(

        @Schema(description = "Unique foundation identifier in ms-tiendas", example = "3")
        Long id,

        @Schema(description = "Institutional name of the foundation", example = "Fundación Abrigo Solidario")
        String nombre,

        @Schema(description = "Neighborhood or zone in Bogota", example = "Chapinero")
        String barrio,

        @Schema(description = "Brief description of the foundation's mission", example = "Recolección de prendas en buen estado para comunidades vulnerables de Bogotá")
        String descripcion,

        @Schema(description = "Physical drop-off address", example = "Calle 85 # 15-32, Chapinero, Bogotá")
        String direccion,

        @Schema(description = "Contact phone number", example = "+57 601 742 8899")
        String telefono,

        @Schema(description = "Contact email address", example = "contacto@abrigosolidario.org")
        String email,

        @Schema(description = "Accepted garment categories", example = "[\"SUPERIOR\", \"INFERIOR\", \"CALZADO\"]")
        List<String> categoriasAceptadas,

        @Schema(description = "Supported social causes", example = "Niñez vulnerable, población desplazada y adultos mayores en Bogotá")
        String causasApoyadas,

        @Schema(description = "Operating hours for physical deliveries", example = "Lunes a Sábado de 08:00 a 17:00")
        String horarioAtencion,

        @Schema(description = "Garment reception policies and guidelines", example = "Se reciben prendas limpias en estado Bueno, Muy bueno o Como nuevo")
        String politicasDonacion,

        @Schema(description = "Indicates whether the foundation is active and enabled to receive donations", example = "true")
        boolean activa,

        @Schema(description = "Representative user ID in ms-auth / ms-usuarios linked to this foundation", example = "24")
        Long representanteId
) {
}
