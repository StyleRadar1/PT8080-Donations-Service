package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.entity;

import java.time.LocalDateTime;

import edu.dosw.styleradar.donaciones.domain.model.OutboxEventStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA para la tabla eventos_outbox_donacion.
 */
@Entity
@Table(
        name = "eventos_outbox_donacion",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_outbox_solicitud_tipo",
                columnNames = {"solicitud_id", "tipo_evento"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonationOutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "solicitud_id", nullable = false)
    private Long requestId;

    @Column(name = "publicacion_id")
    private Long publicationId;

    @Column(name = "fundacion_id", nullable = false)
    private Long foundationId;

    @Column(name = "donante_id", nullable = false)
    private Long donorId;

    @Column(name = "motivo_rechazo", nullable = false, length = 500)
    private String rejectionReason;

    @Column(name = "tipo_evento", nullable = false, length = 80)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private OutboxEventStatus status;

    @Column(name = "intentos", nullable = false)
    private int attempts;

    @Column(name = "ultimo_error", length = 500)
    private String lastError;

    @Column(name = "reclamado_por", length = 100)
    private String claimedBy;

    @Column(name = "fecha_reclamo")
    private LocalDateTime claimedAt;

    @Column(name = "fecha_ocurrencia")
    private LocalDateTime occurredAt;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "fecha_envio")
    private LocalDateTime sentAt;
}
