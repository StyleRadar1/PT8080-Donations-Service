-- =============================================================================
-- V8__extend_donation_outbox_for_delivery_events.sql
-- StyleRadar - Paquete 5: ms-segunda-vida-donaciones
-- Extiende eventos_outbox_donacion para persistir entregas confirmadas
-- (Notificaciones + Estadisticas) de manera independiente y conservar
-- event_id + occurredAt (fecha_ocurrencia) inmutables entre reintentos.
-- =============================================================================

ALTER TABLE eventos_outbox_donacion
    ADD COLUMN IF NOT EXISTS publicacion_id BIGINT,
    ADD COLUMN IF NOT EXISTS fecha_ocurrencia TIMESTAMP WITH TIME ZONE;
