package edu.dosw.styleradar.donaciones.domain.model;

/**
 * Estado de procesamiento de un evento registrado en el Outbox transaccional.
 */
public enum OutboxEventStatus {
    PENDING,
    PROCESSING,
    SENT
}
