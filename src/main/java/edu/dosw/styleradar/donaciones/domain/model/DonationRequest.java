package edu.dosw.styleradar.donaciones.domain.model;

import java.time.LocalDateTime;

import edu.dosw.styleradar.donaciones.domain.exception.InvalidDonationRequestException;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidStateTransitionException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad de dominio que gestiona la solicitud de donación benéfica y la cita logística de entrega a una fundación.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonationRequest {

    private Long id;
    private Long publicationId;
    private Long donorId;
    private Long foundationId;
    private String message;
    private DonationRequestStatus status;
    private String rejectionReason;
    private String deliveryPoint;
    private LocalDateTime pickupStartTime;
    private LocalDateTime pickupEndTime;
    private LocalDateTime requestedAt;
    private LocalDateTime decidedAt;
    private LocalDateTime receivedAt;

    /**
     * Valida las invariantes requeridas al radicar una nueva solicitud de donación.
     */
    public void validateInitialInvariants() {
        if (publicationId == null) {
            throw new InvalidDonationRequestException("El ID de la publicación es obligatorio");
        }
        if (donorId == null) {
            throw new InvalidDonationRequestException("El ID del donante es obligatorio");
        }
        if (foundationId == null) {
            throw new InvalidDonationRequestException("El ID de la fundación destinataria es obligatorio");
        }
    }

    /**
     * Inicializa la solicitud en estado PENDIENTE de evaluación por parte de la fundación benéfica.
     */
    public void initiate() {
        validateInitialInvariants();
        this.status = DonationRequestStatus.PENDING;
        this.requestedAt = LocalDateTime.now();
    }

    /**
     * Alias compatible con el diagrama de clases para inicializar la solicitud.
     */
    public void create() {
        initiate();
    }

    /**
     * Aprueba la solicitud por parte del representante de la fundación benéfica.
     */
    public void accept() {
        if (status != DonationRequestStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    "No se puede aprobar una solicitud que no esté en estado PENDIENTE. Estado actual: " + status);
        }
        this.status = DonationRequestStatus.APPROVED;
        this.decidedAt = LocalDateTime.now();
    }

    /**
     * Alias compatible con el diagrama de arquitectura hexagonal para aprobar la solicitud.
     */
    public void approve() {
        accept();
    }

    /**
     * Rechaza la solicitud de donación indicando obligatoriamente el motivo de la decisión.
     */
    public void reject(String reason) {
        if (status != DonationRequestStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    "No se puede rechazar una solicitud que no esté en estado PENDIENTE. Estado actual: " + status);
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new InvalidDonationRequestException("El motivo de rechazo es obligatorio");
        }
        this.status = DonationRequestStatus.REJECTED;
        this.rejectionReason = reason.trim();
        this.decidedAt = LocalDateTime.now();
    }

    /**
     * Coordina y programa la cita logística de recogida o punto de entrega de las prendas.
     */
    public void schedulePickup(String point, LocalDateTime start, LocalDateTime end) {
        if (status != DonationRequestStatus.APPROVED && status != DonationRequestStatus.IN_PICKUP) {
            throw new InvalidStateTransitionException(
                    "Solo se puede programar o reprogramar la recogida de una solicitud aprobada. Estado actual: " + status);
        }
        if (point == null || point.trim().isEmpty()) {
            throw new InvalidDonationRequestException("El punto de entrega o recogida es obligatorio");
        }
        if (start == null || end == null) {
            throw new InvalidDonationRequestException("Las fechas de la franja horaria son obligatorias");
        }
        if (!end.isAfter(start)) {
            throw new InvalidDonationRequestException("El fin de la franja horaria debe ser posterior al inicio");
        }

        this.deliveryPoint = point.trim();
        this.pickupStartTime = start;
        this.pickupEndTime = end;
        this.status = DonationRequestStatus.IN_PICKUP;
    }

    /**
     * Confirma la entrega efectiva de la donación a la sede o personal de la fundación.
     */
    public void confirmDelivery() {
        if (status != DonationRequestStatus.IN_PICKUP) {
            throw new InvalidStateTransitionException(
                    "Solo se puede confirmar la entrega de una solicitud en proceso de recogida. Estado actual: " + status);
        }
        this.status = DonationRequestStatus.DELIVERED;
        this.receivedAt = LocalDateTime.now();
    }
}
