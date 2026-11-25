package edu.dosw.styleradar.donaciones.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO para la decisión de una fundación sobre una solicitud de donación (aprobar o rechazar).
 */
@Schema(description = "Request payload for a foundation to approve or reject a donation request")
public record DecideDonationRequestDTO(

        @Schema(description = "ID of the foundation making the decision", example = "501")
        @NotNull(message = "El ID de la fundación es obligatorio")
        Long foundationId,

        @Schema(description = "Indicates whether the donation request is approved (true) or rejected (false)", example = "true")
        @NotNull(message = "La decisión de aprobación es obligatoria")
        Boolean approve,

        @Schema(description = "Rejection reason required when declining the request", example = "Capacidad de almacenamiento colmada para esta categoría")
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
