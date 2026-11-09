package edu.dosw.styleradar.donaciones.application.dto.response;

import java.time.LocalDateTime;

import edu.dosw.styleradar.donaciones.domain.model.DonationRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de respuesta que representa los datos de una solicitud de donación.
 */
@Schema(description = "Respuesta detallada con información de una solicitud de donación")
public record DonationRequestResponseDTO(

        @Schema(description = "Identificador único de la solicitud de donación", example = "1")
        Long id,

        @Schema(description = "ID de la publicación vinculada", example = "1")
        Long publicationId,

        @Schema(description = "ID del donante", example = "101")
        Long donorId,

        @Schema(description = "ID de la fundación destinataria", example = "501")
        Long foundationId,

        @Schema(description = "Mensaje del donante para la fundación", example = "Deseo donar esta chaqueta")
        String message,

        @Schema(description = "Estado actual de la solicitud", example = "PENDING")
        DonationRequestStatus status,

        @Schema(description = "Motivo de rechazo en caso de no ser aceptada", example = "Sin capacidad")
        String rejectionReason,

        @Schema(description = "Punto o dirección de entrega acordada", example = "Calle 100 # 15-20, Bogotá")
        String deliveryPoint,

        @Schema(description = "Inicio de franja horaria agendada", example = "2026-11-15T09:00:00")
        LocalDateTime slotStart,

        @Schema(description = "Fin de franja horaria agendada", example = "2026-11-15T12:00:00")
        LocalDateTime slotEnd,

        @Schema(description = "Fecha y hora de creación de la solicitud", example = "2026-11-06T15:00:00")
        LocalDateTime requestDate,

        @Schema(description = "Fecha y hora de la decisión tomada", example = "2026-11-06T17:30:00")
        LocalDateTime decisionDate,

        @Schema(description = "Fecha y hora de la confirmación de recepción física", example = "2026-11-15T11:45:00")
        LocalDateTime receptionDate
) {
}
