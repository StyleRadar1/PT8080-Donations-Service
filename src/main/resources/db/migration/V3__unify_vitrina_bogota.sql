-- =============================================================================
-- V3__unify_vitrina_bogota.sql
-- StyleRadar - Paquete 5: ms-segunda-vida-donaciones (Sprint 3)
-- Unificación del Modelo de Economía Circular:
-- 1. Discriminador 'es_donacion' (TRUE: donación precio 0 COP; FALSE: segunda mano C2C)
-- 2. Restricción territorial: 20 localidades oficiales de Bogotá D.C.
-- 3. Concurrencia optimista (@Version) para reclamos concurrentes en vitrina
-- =============================================================================

-- 1. Agregar columnas a publicaciones_segunda_mano
ALTER TABLE publicaciones_segunda_mano
    ADD COLUMN IF NOT EXISTS es_donacion BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS localidad VARCHAR(50) NOT NULL DEFAULT 'CHAPINERO',
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- 2. Agregar columna version a solicitudes_donacion
ALTER TABLE solicitudes_donacion
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- 3. Constraint de verificación para las 20 localidades oficiales de Bogotá D.C.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_pub_localidad'
    ) THEN
        ALTER TABLE publicaciones_segunda_mano
            ADD CONSTRAINT chk_pub_localidad CHECK (
                localidad IN (
                    'USAQUEN', 'CHAPINERO', 'SANTA_FE', 'SAN_CRISTOBAL', 'USME',
                    'TUNJUELITO', 'BOSA', 'KENNEDY', 'FONTIBON', 'ENGATIVA',
                    'SUBA', 'BARRIOS_UNIDOS', 'TEUSAQUILLO', 'LOS_MARTIRES',
                    'ANTONIO_NARINO', 'PUENTE_ARANDA', 'LA_CANDELARIA',
                    'RAFAEL_URIBE_URIBE', 'CIUDAD_BOLIVAR', 'SUMAPAZ'
                )
            );
    END IF;
END $$;

-- 4. Constraint de verificación de precio según discriminador de donación
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_pub_es_donacion_precio'
    ) THEN
        ALTER TABLE publicaciones_segunda_mano
            ADD CONSTRAINT chk_pub_es_donacion_precio CHECK (
                (es_donacion = TRUE AND precio = 0) OR
                (es_donacion = FALSE AND precio > 0)
            );
    END IF;
END $$;

-- 5. Índices de rendimiento para Vitrina Abierta y filtros de búsqueda
CREATE INDEX IF NOT EXISTS idx_pub_es_donacion ON publicaciones_segunda_mano (es_donacion);
CREATE INDEX IF NOT EXISTS idx_pub_localidad ON publicaciones_segunda_mano (localidad);
CREATE INDEX IF NOT EXISTS idx_pub_donacion_vitrina ON publicaciones_segunda_mano (es_donacion, estado);
