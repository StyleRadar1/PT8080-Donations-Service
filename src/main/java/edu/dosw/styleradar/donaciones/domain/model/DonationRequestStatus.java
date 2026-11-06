package edu.dosw.styleradar.donaciones.domain.model;

import java.util.Set;

/**
 * Estados del ciclo de vida de una solicitud de donación hacia una fundación aliada.
 */
public enum DonationRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    IN_PICKUP,
    DELIVERED;

    /** Conjunto de estados que determinan que una solicitud está activa y en curso. */
    public static final Set<DonationRequestStatus> ACTIVE_STATUSES = Set.of(PENDING, APPROVED, IN_PICKUP);

    /**
     * Determina si la solicitud se encuentra en curso (bloqueando solicitudes concurrentes sobre la prenda).
     */
    public boolean isInProgress() {
        return ACTIVE_STATUSES.contains(this);
    }

    /**
     * Determina si la solicitud ya alcanzó un estado terminal o concluido.
     */
    public boolean isFinished() {
        return this == REJECTED || this == DELIVERED;
    }
}
