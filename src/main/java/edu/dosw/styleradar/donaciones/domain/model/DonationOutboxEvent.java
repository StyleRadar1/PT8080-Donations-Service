package edu.dosw.styleradar.donaciones.domain.model;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Registro de dominio del Outbox transaccional para garantizar la entrega de avisos
 * de cambio de estado por cada solicitud afectada.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonationOutboxEvent {

    public static final String TYPE_DONATION_CANCELLED_FOUNDATION_DELETED = "DONATION_CANCELLED_FOUNDATION_DELETED";
    private static final int MAX_ERROR_LENGTH = 500;

    private Long id;
    private Long requestId;
    private Long foundationId;
    private Long donorId;
    private String rejectionReason;
    private String eventType;
    private OutboxEventStatus status;
    private int attempts;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;

    public static DonationOutboxEvent pendingFoundationCancellation(DonationRequest request) {
        return DonationOutboxEvent.builder()
                .requestId(request.getId())
                .foundationId(request.getFoundationId())
                .donorId(request.getDonorId())
                .rejectionReason(request.getRejectionReason())
                .eventType(TYPE_DONATION_CANCELLED_FOUNDATION_DELETED)
                .status(OutboxEventStatus.PENDING)
                .attempts(0)
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();
    }

    public void markSent() {
        this.attempts = this.attempts + 1;
        this.status = OutboxEventStatus.SENT;
        this.lastError = null;
        this.sentAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public void recordFailure(String errorMessage) {
        this.attempts = this.attempts + 1;
        this.status = OutboxEventStatus.PENDING;
        if (errorMessage == null || errorMessage.isBlank()) {
            this.lastError = "Error desconocido al publicar en broker";
        } else {
            String trimmed = errorMessage.trim();
            this.lastError = trimmed.length() > MAX_ERROR_LENGTH
                    ? trimmed.substring(0, MAX_ERROR_LENGTH)
                    : trimmed;
        }
    }
}
