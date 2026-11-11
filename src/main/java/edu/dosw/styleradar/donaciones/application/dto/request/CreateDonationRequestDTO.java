package edu.dosw.styleradar.donaciones.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO para la solicitud de creación de una petición de donación hacia una fundación.
 */
@Schema(description = "Datos para crear una nueva solicitud de donación de una prenda")
public record CreateDonationRequestDTO(

        @Schema(description = "ID de la publicación de segunda mano a donar", example = "1")
        @NotNull(message = "El ID de la publicación es obligatorio")
        Long publicationId,

        @Schema(description = "ID del usuario donante propietario de la prenda", example = "101")
        @NotNull(message = "El ID del donante es obligatorio")
        Long donorId,

        @Schema(description = "ID de la fundación destinataria", example = "501")
        @NotNull(message = "El ID de la fundación es obligatorio")
        Long foundationId,

        @Schema(description = "Mensaje opcional del donante para la fundación", example = "Deseo donar esta chaqueta para las campañas de invierno")
        @Size(max = 500, message = "El mensaje no puede exceder 500 caracteres")
        String message
) {
}
