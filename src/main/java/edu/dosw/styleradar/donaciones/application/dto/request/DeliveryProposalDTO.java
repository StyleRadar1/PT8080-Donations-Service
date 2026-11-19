package edu.dosw.styleradar.donaciones.application.dto.request;

import java.time.LocalDateTime;

import edu.dosw.styleradar.donaciones.domain.model.DeliveryMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO anidado que representa la propuesta inicial de entrega o recogida de la donación
 * (modo de entrega, punto/dirección y franja horaria).
 */
@Schema(description = "Initial delivery or pickup proposal submitted by the donor")
public record DeliveryProposalDTO(

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
