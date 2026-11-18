package edu.dosw.styleradar.donaciones.application.dto.request;

import java.time.LocalDateTime;

import edu.dosw.styleradar.donaciones.domain.model.DeliveryMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO para la solicitud de creación de una petición de donación hacia una fundación,
 * incluyendo los datos obligatorios de propuesta de entrega (SR-RF-45 / SR-RF-48).
 */
@Schema(description = "Request payload to create a new garment donation request with an initial delivery proposal")
public record CreateDonationRequestDTO(

        @Schema(description = "ID of the second-hand publication to donate", example = "1")
        @NotNull(message = "El ID de la publicación es obligatorio")
        Long publicationId,

        @Schema(description = "ID of the donor user owning the garment", example = "101")
        @NotNull(message = "El ID del donante es obligatorio")
        Long donorId,

        @Schema(description = "ID of the target foundation", example = "3")
        @NotNull(message = "El ID de la fundación es obligatorio")
        Long foundationId,

        @Schema(description = "Optional message from the donor to the foundation", example = "Deseo donar esta chaqueta para las campañas de invierno")
        @Size(max = 500, message = "El mensaje no puede exceder 500 caracteres")
        String message,

        @Schema(description = "Proposed delivery mode by the donor (HOME_PICKUP or DROP_OFF_POINT)", example = "DROP_OFF_POINT")
        @NotNull(message = "El modo de entrega propuesto es obligatorio")
        DeliveryMode deliveryMode,

        @Schema(description = "Proposed drop-off point or pickup address in Bogota D.C.", example = "Calle 85 # 15-32, Chapinero, Bogotá")
        @NotBlank(message = "El punto de entrega o recogida propuesto es obligatorio")
        @Size(max = 255, message = "El punto de entrega no puede exceder 255 caracteres")
        String deliveryPoint,

        @Schema(description = "Start timestamp of the proposed delivery time window", example = "2026-12-15T09:00:00")
        @NotNull(message = "La fecha y hora de inicio de la franja es obligatoria")
        LocalDateTime startTime,

        @Schema(description = "End timestamp of the proposed delivery time window", example = "2026-12-15T12:00:00")
        @NotNull(message = "La fecha y hora de fin de la franja es obligatoria")
        LocalDateTime endTime
) {
}
