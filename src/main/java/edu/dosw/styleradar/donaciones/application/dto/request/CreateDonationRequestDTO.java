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
@Schema(description = "Datos para crear una nueva solicitud de donación de una prenda con su propuesta inicial de entrega")
public record CreateDonationRequestDTO(

        @Schema(description = "ID de la publicación de segunda mano a donar", example = "1")
        @NotNull(message = "El ID de la publicación es obligatorio")
        Long publicationId,

        @Schema(description = "ID del usuario donante propietario de la prenda", example = "101")
        @NotNull(message = "El ID del donante es obligatorio")
        Long donorId,

        @Schema(description = "ID de la fundación destinataria", example = "3")
        @NotNull(message = "El ID de la fundación es obligatorio")
        Long foundationId,

        @Schema(description = "Mensaje opcional del donante para la fundación", example = "Deseo donar esta chaqueta para las campañas de invierno")
        @Size(max = 500, message = "El mensaje no puede exceder 500 caracteres")
        String message,

        @Schema(description = "Modalidad de entrega propuesta por el donante (HOME_PICKUP o DROP_OFF_POINT)", example = "DROP_OFF_POINT")
        @NotNull(message = "El modo de entrega propuesto es obligatorio")
        DeliveryMode deliveryMode,

        @Schema(description = "Punto de acopio o dirección en Bogotá D.C. propuesta para la entrega", example = "Calle 85 # 15-32, Chapinero, Bogotá")
        @NotBlank(message = "El punto de entrega o recogida propuesto es obligatorio")
        @Size(max = 255, message = "El punto de entrega no puede exceder 255 caracteres")
        String deliveryPoint,

        @Schema(description = "Fecha y hora de inicio de la franja horaria propuesta", example = "2026-12-15T09:00:00")
        @NotNull(message = "La fecha y hora de inicio de la franja es obligatoria")
        LocalDateTime startTime,

        @Schema(description = "Fecha y hora de fin de la franja horaria propuesta", example = "2026-12-15T12:00:00")
        @NotNull(message = "La fecha y hora de fin de la franja es obligatoria")
        LocalDateTime endTime
) {
}
