-- =============================================================================
-- V7__harden_donation_outbox_delivery.sql
-- StyleRadar - Paquete 5: ms-segunda-vida-donaciones
-- Endurece el Outbox transaccional con:
-- 1) event_id estable para idempotencia exacta entre reintentos
-- 2) reclamado_por / fecha_reclamo para reclamación atómica concurrente y expiración de lease
-- =============================================================================

ALTER TABLE eventos_outbox_donacion
    ADD COLUMN IF NOT EXISTS event_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS reclamado_por VARCHAR(100),
    ADD COLUMN IF NOT EXISTS fecha_reclamo TIMESTAMP WITH TIME ZONE;

UPDATE eventos_outbox_donacion
SET event_id = CAST(id AS VARCHAR(64))
WHERE event_id IS NULL;

ALTER TABLE eventos_outbox_donacion
    ALTER COLUMN event_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_outbox_estado_reclamo
    ON eventos_outbox_donacion (estado, fecha_reclamo, fecha_creacion);
