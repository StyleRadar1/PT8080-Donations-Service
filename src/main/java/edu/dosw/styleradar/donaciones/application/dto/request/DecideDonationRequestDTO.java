package edu.dosw.styleradar.donaciones.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO para la decisión de una fundación sobre una solicitud de donación (aprobar o rechazar).
 */
@Schema(description = "Datos para que la fundación apruebe o rechace una solicitud de donación")
public record DecideDonationRequestDTO(

        @Schema(description = "ID de la fundación que toma la decisión", example = "501")
        @NotNull(message = "El ID de la fundación es obligatorio")
        Long foundationId,

        @Schema(description = "Indica si la donación es aprobada (true) o rechazada (false)", example = "true")
        @NotNull(message = "La decisión de aprobación es obligatoria")
        Boolean approve,

        @Schema(description = "Motivo del rechazo en caso de no ser aceptada", example = "Capacidad de almacenamiento colmada para esta categoría")
        @Size(max = 500, message = "El motivo de rechazo no puede exceder 500 caracteres")
        String reason
) {
    public Boolean approved() {
        return approve;
    }

    public String rejectionReason() {
        return reason;
    }
}
