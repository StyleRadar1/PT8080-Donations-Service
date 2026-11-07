package edu.dosw.styleradar.donaciones.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para confirmar la recepción física de la prenda en la fundación.
 */
@Schema(description = "Datos para que la fundación confirme la recepción física de la prenda donada")
public record ConfirmReceiptRequestDTO(

        @Schema(description = "ID de la fundación que confirma la recepción", example = "501")
        @NotNull(message = "El ID de la fundación es obligatorio")
        Long foundationId
) {
}
