-- =============================================================================
-- V4__add_delivery_mode_to_donations.sql
-- StyleRadar - Paquete 5: ms-segunda-vida-donaciones (Sprint 3)
-- Incorpora la modalidad de entrega propuesta en solicitudes de donación (SR-RF-45)
-- =============================================================================

ALTER TABLE solicitudes_donacion
    ADD COLUMN IF NOT EXISTS modo_entrega VARCHAR(30);
