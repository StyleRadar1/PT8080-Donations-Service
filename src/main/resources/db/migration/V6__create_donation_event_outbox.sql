-- =============================================================================
-- V6__create_donation_event_outbox.sql
-- StyleRadar - Paquete 5: ms-segunda-vida-donaciones
-- Tabla transaccional Outbox para garantizar entrega confiable e idempotente
-- de notificaciones tras cancelaciones automáticas (p. ej. foundation.deleted).
-- =============================================================================

CREATE TABLE IF NOT EXISTS eventos_outbox_donacion (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    solicitud_id BIGINT NOT NULL,
    fundacion_id BIGINT NOT NULL,
    donante_id BIGINT NOT NULL,
    motivo_rechazo VARCHAR(500) NOT NULL,
    tipo_evento VARCHAR(80) NOT NULL DEFAULT 'DONATION_CANCELLED_FOUNDATION_DELETED',
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    intentos INTEGER NOT NULL DEFAULT 0,
    ultimo_error VARCHAR(500),
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_envio TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_outbox_solicitud_tipo UNIQUE (solicitud_id, tipo_evento)
);

CREATE INDEX IF NOT EXISTS idx_outbox_estado_fecha ON eventos_outbox_donacion (estado, fecha_creacion);
CREATE INDEX IF NOT EXISTS idx_outbox_fundacion ON eventos_outbox_donacion (fundacion_id);
