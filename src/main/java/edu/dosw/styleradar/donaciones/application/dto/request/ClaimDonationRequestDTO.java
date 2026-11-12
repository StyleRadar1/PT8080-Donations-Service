package edu.dosw.styleradar.donaciones.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO para la solicitud de reclamo de una prenda benéfica desde la Vitrina Abierta de Donaciones.
 */
@Schema(description = "Datos para que una fundación benéfica aliada reclame una prenda disponible en la vitrina pública")
public record ClaimDonationRequestDTO(

        @Schema(description = "ID de la publicación de donación disponible en la vitrina", example = "10")
        @NotNull(message = "El ID de la publicación es obligatorio")
        Long publicationId,

        @Schema(description = "ID de la fundación receptora que reclama la prenda", example = "20")
        @NotNull(message = "El ID de la fundación es obligatorio")
        Long foundationId,

        @Schema(description = "Mensaje opcional de necesidad o motivación por parte de la fundación", example = "Solicitamos esta chaqueta para nuestro programa de abrigo a la niñez en Ciudad Bolívar")
        @Size(max = 500, message = "El mensaje no puede exceder 500 caracteres")
        String message
) {
}
