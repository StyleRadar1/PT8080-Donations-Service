package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.entity;

import java.time.LocalDateTime;

import edu.dosw.styleradar.donaciones.domain.model.DonationRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA para la tabla solicitudes_donacion.
 */
@Entity
@Table(name = "solicitudes_donacion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonationRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publicacion_id", nullable = false)
    private SecondHandPublicationEntity publication;

    @Column(name = "donante_id", nullable = false)
    private Long donorId;

    @Column(name = "fundacion_id", nullable = false)
    private Long foundationId;

    @Column(name = "mensaje", length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private DonationRequestStatus status;

    @Column(name = "motivo_rechazo", length = 500)
    private String rejectionReason;

    @Column(name = "punto_entrega", length = 255)
    private String deliveryPoint;

    @Column(name = "franja_inicio")
    private LocalDateTime pickupStartTime;

    @Column(name = "franja_fin")
    private LocalDateTime pickupEndTime;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "fecha_decision")
    private LocalDateTime decidedAt;

    @Column(name = "fecha_recepcion")
    private LocalDateTime receivedAt;
}
