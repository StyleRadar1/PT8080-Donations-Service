package edu.dosw.styleradar.donaciones.application.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de respuesta para poblar el selector desplegable de fundaciones aliadas activas (SR-RF-44).
 */
@Schema(description = "Información pública de una fundación aliada activa disponible para recibir donaciones")
public record FoundationResponseDTO(

        @Schema(description = "Identificador único de la fundación en ms-tiendas", example = "3")
        Long id,

        @Schema(description = "Nombre institucional de la fundación", example = "Fundación Abrigo Solidario")
        String nombre,

        @Schema(description = "Barrio o zona en Bogotá", example = "Chapinero")
        String barrio,

        @Schema(description = "Descripción breve de la labor de la fundación", example = "Recolección de prendas en buen estado para comunidades vulnerables de Bogotá")
        String descripcion,

        @Schema(description = "Dirección física de recepción", example = "Calle 85 # 15-32, Chapinero, Bogotá")
        String direccion,

        @Schema(description = "Teléfono de contacto", example = "+57 601 742 8899")
        String telefono,

        @Schema(description = "Correo de contacto", example = "contacto@abrigosolidario.org")
        String email,

        @Schema(description = "Categorías de prendas aceptadas", example = "[\"SUPERIOR\", \"INFERIOR\", \"CALZADO\"]")
        List<String> categoriasAceptadas,

        @Schema(description = "Causas sociales apoyadas", example = "Niñez vulnerable, población desplazada y adultos mayores en Bogotá")
        String causasApoyadas,

        @Schema(description = "Horario de atención para entregas", example = "Lunes a Sábado de 08:00 a 17:00")
        String horarioAtencion,

        @Schema(description = "Políticas o condiciones de recepción de prendas", example = "Se reciben prendas limpias en estado Bueno, Muy bueno o Como nuevo")
        String politicasDonacion,

        @Schema(description = "Indica si la fundación está activa y habilitada para recibir donaciones", example = "true")
        boolean activa,

        @Schema(description = "ID del usuario representante en ms-auth / ms-usuarios vinculado a esta fundación", example = "24")
        Long representanteId
) {
}
