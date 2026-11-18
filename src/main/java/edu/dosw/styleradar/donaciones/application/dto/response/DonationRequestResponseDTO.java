package edu.dosw.styleradar.donaciones.application.dto.response;

import java.time.LocalDateTime;

import edu.dosw.styleradar.donaciones.domain.model.DeliveryMode;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de respuesta que representa los datos de una solicitud de donación benéfica en Bogotá D.C.
 */
@Schema(description = "Detailed response representing a charitable donation request")
public record DonationRequestResponseDTO(

        @Schema(description = "Unique donation request identifier", example = "1")
        Long id,

        @Schema(description = "Linked publication ID", example = "1")
        Long publicationId,

        @Schema(description = "ID of the donor (user, store, or foundation)", example = "101")
        Long donorId,

        @Schema(description = "ID of the target or receiving foundation", example = "501")
        Long foundationId,

        @Schema(description = "Message from the donor or claiming foundation", example = "Deseo donar esta chaqueta para el programa de abrigo")
        String message,

        @Schema(description = "Current lifecycle status of the donation request", example = "APPROVED")
        DonationRequestStatus status,

        @Schema(description = "Rejection reason if declined", example = "Sin capacidad")
        String rejectionReason,

        @Schema(description = "Proposed or agreed delivery mode (HOME_PICKUP or DROP_OFF_POINT)", example = "DROP_OFF_POINT")
        DeliveryMode deliveryMode,

        @Schema(description = "Coordinated delivery point or address in Bogota D.C.", example = "Sede Chapinero: Calle 67 # 11-45, Bogotá")
        String deliveryPoint,

        @Schema(description = "Scheduled pickup time window start", example = "2026-11-15T09:00:00")
        LocalDateTime slotStart,

        @Schema(description = "Scheduled pickup time window end", example = "2026-11-15T12:00:00")
        LocalDateTime slotEnd,

        @Schema(description = "Version number for optimistic concurrency control", example = "0")
        Long version,

        @Schema(description = "Request creation timestamp", example = "2026-11-06T15:00:00")
        LocalDateTime requestDate,

        @Schema(description = "Decision timestamp", example = "2026-11-06T17:30:00")
        LocalDateTime decisionDate,

        @Schema(description = "Physical delivery confirmation timestamp", example = "2026-11-15T11:45:00")
        LocalDateTime receptionDate,

        @Schema(description = "Verifiable donation receipt code issued upon delivery confirmation (SR-DON-XXXX)", example = "SR-DON-0001")
        String receiptCode
) {
    /**
     * Constructor de compatibilidad sin receiptCode ni deliveryMode explícitos.
     */
    public DonationRequestResponseDTO(
            Long id,
            Long publicationId,
            Long donorId,
            Long foundationId,
            String message,
            DonationRequestStatus status,
            String rejectionReason,
            String deliveryPoint,
            LocalDateTime slotStart,
            LocalDateTime slotEnd,
            Long version,
            LocalDateTime requestDate,
            LocalDateTime decisionDate,
            LocalDateTime receptionDate) {
        this(
                id, publicationId, donorId, foundationId, message, status,
                rejectionReason, null, deliveryPoint, slotStart, slotEnd, version,
                requestDate, decisionDate, receptionDate,
                generateReceiptCode(id, status)
        );
    }

    /**
     * Genera el código verificable de comprobante cuando la donación ha sido confirmada como entregada.
     */
    public static String generateReceiptCode(Long id, DonationRequestStatus status) {
        if (id == null || status != DonationRequestStatus.DELIVERED) {
            return null;
        }
        return String.format("SR-DON-%04d", id);
    }
}
