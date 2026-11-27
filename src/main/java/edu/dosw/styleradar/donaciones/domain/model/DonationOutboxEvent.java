package edu.dosw.styleradar.donaciones.domain.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Registro de dominio del Outbox transaccional para garantizar la entrega de avisos
 * de cambio de estado por cada solicitud afectada, conservando {@code eventId} y {@code occurredAt}
 * estables entre reintentos con soporte de reclamación atómica.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonationOutboxEvent {

    public static final String TYPE_DONATION_CANCELLED_FOUNDATION_DELETED = "DONATION_CANCELLED_FOUNDATION_DELETED";
    public static final String TYPE_DONATION_DELIVERED_NOTIFICATIONS = "DONATION_DELIVERED_NOTIFICATIONS";
    public static final String TYPE_DONATION_DELIVERED_STATISTICS = "DONATION_DELIVERED_STATISTICS";
    private static final int MAX_ERROR_LENGTH = 500;

    private Long id;
    private String eventId;
    private Long requestId;
    private Long publicationId;
    private Long foundationId;
    private Long donorId;
    private String rejectionReason;
    private String eventType;
    private OutboxEventStatus status;
    private int attempts;
    private String lastError;
    private String claimedBy;
    private LocalDateTime claimedAt;
    private LocalDateTime occurredAt;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;

    public static DonationOutboxEvent pendingFoundationCancellation(DonationRequest request) {
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        return DonationOutboxEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .requestId(request.getId())
                .publicationId(request.getPublicationId())
                .foundationId(request.getFoundationId())
                .donorId(request.getDonorId())
                .rejectionReason(request.getRejectionReason())
                .eventType(TYPE_DONATION_CANCELLED_FOUNDATION_DELETED)
                .status(OutboxEventStatus.PENDING)
                .attempts(0)
                .occurredAt(nowUtc.truncatedTo(ChronoUnit.SECONDS))
                .createdAt(nowUtc)
                .build();
    }

    public static DonationOutboxEvent pendingDeliveryNotifications(DonationRequest request, Instant occurredAt) {
        return pendingDeliveryEvent(
                request,
                occurredAt,
                TYPE_DONATION_DELIVERED_NOTIFICATIONS,
                "Recepción física de donación confirmada");
    }

    public static DonationOutboxEvent pendingDeliveryStatistics(DonationRequest request, Instant occurredAt) {
        return pendingDeliveryEvent(
                request,
                occurredAt,
                TYPE_DONATION_DELIVERED_STATISTICS,
                "Donación completada para estadísticas");
    }

    private static DonationOutboxEvent pendingDeliveryEvent(
            DonationRequest request,
            Instant occurredAt,
            String eventType,
            String detail) {
        Instant effectiveInstant = (occurredAt != null ? occurredAt : Instant.now()).truncatedTo(ChronoUnit.SECONDS);
        return DonationOutboxEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .requestId(request.getId())
                .publicationId(request.getPublicationId())
                .foundationId(request.getFoundationId())
                .donorId(request.getDonorId() != null ? request.getDonorId() : 0L)
                .rejectionReason(detail)
                .eventType(eventType)
                .status(OutboxEventStatus.PENDING)
                .attempts(0)
                .occurredAt(LocalDateTime.ofInstant(effectiveInstant, ZoneOffset.UTC))
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();
    }

    public void markSent() {
        this.attempts = this.attempts + 1;
        this.status = OutboxEventStatus.SENT;
        this.lastError = null;
        this.claimedBy = null;
        this.claimedAt = null;
        this.sentAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public void recordFailure(String errorMessage) {
        this.attempts = this.attempts + 1;
        this.status = OutboxEventStatus.PENDING;
        this.claimedBy = null;
        this.claimedAt = null;
        this.lastError = normalizeError(errorMessage);
    }

    public static String normalizeError(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return "Error desconocido al publicar en broker";
        }
        String trimmed = errorMessage.trim();
        return trimmed.length() > MAX_ERROR_LENGTH
                ? trimmed.substring(0, MAX_ERROR_LENGTH)
                : trimmed;
    }
}
