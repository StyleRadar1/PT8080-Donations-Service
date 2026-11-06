package edu.dosw.styleradar.donaciones.domain.model;

import java.util.Set;

/**
 * Estados del flujo y máquina de estados de una solicitud de donación hacia una fundación aliada.
 */
public enum EstadoSolicitudDonacion {
    PENDIENTE,
    APROBADA,
    RECHAZADA,
    EN_RECOGIDA,
    ENTREGADA;

    /** Conjunto de estados que denotan una solicitud activa o en proceso. */
    public static final Set<EstadoSolicitudDonacion> ESTADOS_EN_CURSO = Set.of(PENDIENTE, APROBADA, EN_RECOGIDA);

    /**
     * Determina si la solicitud se encuentra en curso bloqueando nuevas solicitudes para la misma publicación.
     */
    public boolean isEnCurso() {
        return ESTADOS_EN_CURSO.contains(this);
    }

    /**
     * Determina si la solicitud ya ha concluido su ciclo de vida de forma terminal.
     */
    public boolean isFinalizada() {
        return this == RECHAZADA || this == ENTREGADA;
    }
}
