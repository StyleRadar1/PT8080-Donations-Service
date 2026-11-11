package edu.dosw.styleradar.donaciones.application.dto.request;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO para la solicitud de programación de recogida o entrega física de una donación.
 */
@Schema(description = "Datos para agendar punto de entrega y franja horaria de recogida de la donación")
public record SchedulePickupRequestDTO(

        @Schema(description = "ID del usuario donante que agenda la recogida", example = "101")
        @NotNull(message = "El ID del usuario donante es obligatorio")
        Long userId,

        @Schema(description = "Punto o dirección de entrega acordada", example = "Calle 100 # 15-20, Bogotá")
        @NotBlank(message = "El punto de entrega es obligatorio")
        @Size(max = 255, message = "El punto de entrega no puede exceder 255 caracteres")
        String deliveryPoint,

        @Schema(description = "Inicio de la franja horaria disponible para la entrega", example = "2026-11-15T09:00:00")
        @NotNull(message = "La fecha y hora de inicio de la franja es obligatoria")
        LocalDateTime startTime,

        @Schema(description = "Fin de la franja horaria disponible para la entrega", example = "2026-11-15T12:00:00")
        @NotNull(message = "La fecha y hora de fin de la franja es obligatoria")
        LocalDateTime endTime
) {
}
