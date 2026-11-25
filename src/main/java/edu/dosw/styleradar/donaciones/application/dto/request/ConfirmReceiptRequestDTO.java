package edu.dosw.styleradar.donaciones.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para confirmar la recepción física de la prenda en la fundación.
 */
@Schema(description = "Request payload for a foundation to confirm physical receipt of a donated garment")
public record ConfirmReceiptRequestDTO(

        @Schema(description = "ID of the foundation confirming receipt", example = "501")
        @NotNull(message = "El ID de la fundación es obligatorio")
        Long foundationId
) {
}
