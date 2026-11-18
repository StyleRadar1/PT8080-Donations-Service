package edu.dosw.styleradar.donaciones.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO para la solicitud de reclamo de una prenda benéfica desde la Vitrina Abierta de Donaciones.
 */
@Schema(description = "Request payload for an allied charity foundation to claim an available garment from the open showcase")
public record ClaimDonationRequestDTO(

        @Schema(description = "ID of the available donation publication in the showcase", example = "10")
        @NotNull(message = "El ID de la publicación es obligatorio")
        Long publicationId,

        @Schema(description = "ID of the receiving foundation claiming the garment", example = "20")
        @NotNull(message = "El ID de la fundación es obligatorio")
        Long foundationId,

        @Schema(description = "Optional motivation or need message from the foundation", example = "Solicitamos esta chaqueta para nuestro programa de abrigo a la niñez en Ciudad Bolívar")
        @Size(max = 500, message = "El mensaje no puede exceder 500 caracteres")
        String message
) {
}
