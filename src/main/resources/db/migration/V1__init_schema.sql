-- =============================================================================
-- V1__init_schema.sql
-- StyleRadar - Paquete 5: ms-segunda-vida-donaciones
-- Inicialización del esquema base para PostgreSQL 16
-- Bounded Context: Economía Circular, Donaciones Benéficas y Citas Logísticas
-- =============================================================================

-- Extensiones necesarias (PostgreSQL 13+ incluye gen_random_uuid() de forma nativa sin extensiones)
-- CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- -----------------------------------------------------------------------------
-- 1. Tabla: articulos_segunda_mano (Ciclo C2C / Resale)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS articulos_segunda_mano (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendedor_id UUID NOT NULL,
    titulo VARCHAR(255) NOT NULL,
    descripcion TEXT,
    talla VARCHAR(50) NOT NULL,
    estado_conservacion VARCHAR(50) NOT NULL,
    precio NUMERIC(12, 2) NOT NULL CHECK (precio >= 0),
    estado_publicacion VARCHAR(50) NOT NULL DEFAULT 'BORRADOR',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para articulos_segunda_mano
CREATE INDEX IF NOT EXISTS idx_articulos_vendedor ON articulos_segunda_mano (vendedor_id);
CREATE INDEX IF NOT EXISTS idx_articulos_estado_publicacion ON articulos_segunda_mano (estado_publicacion);
CREATE INDEX IF NOT EXISTS idx_articulos_created_at ON articulos_segunda_mano (created_at DESC);

-- -----------------------------------------------------------------------------
-- 2. Tabla: solicitudes_donacion (Canal Benéfico a Fundaciones Aliadas)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS solicitudes_donacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    donante_id UUID NOT NULL,
    fundacion_id UUID NOT NULL,
    estado VARCHAR(50) NOT NULL DEFAULT 'PENDIENTE',
    modalidad_entrega VARCHAR(50) NOT NULL,
    fecha_propuesta DATE NOT NULL,
    franja_horaria VARCHAR(50) NOT NULL,
    punto_entrega VARCHAR(255),
    motivo_rechazo TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para solicitudes_donacion
CREATE INDEX IF NOT EXISTS idx_solicitudes_donante ON solicitudes_donacion (donante_id);
CREATE INDEX IF NOT EXISTS idx_solicitudes_fundacion ON solicitudes_donacion (fundacion_id);
CREATE INDEX IF NOT EXISTS idx_solicitudes_estado ON solicitudes_donacion (estado);
CREATE INDEX IF NOT EXISTS idx_solicitudes_fecha_propuesta ON solicitudes_donacion (fecha_propuesta);

-- -----------------------------------------------------------------------------
-- 3. Tabla: citas_recogida (Logística Domiciliaria / Acopio)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS citas_recogida (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    solicitud_id UUID NOT NULL REFERENCES solicitudes_donacion(id) ON DELETE CASCADE,
    direccion VARCHAR(255) NOT NULL,
    fecha_programada DATE NOT NULL,
    franja_horaria VARCHAR(50) NOT NULL,
    estado_logistico VARCHAR(50) NOT NULL DEFAULT 'PROGRAMADA',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para citas_recogida
CREATE INDEX IF NOT EXISTS idx_citas_solicitud ON citas_recogida (solicitud_id);
CREATE INDEX IF NOT EXISTS idx_citas_fecha_programada ON citas_recogida (fecha_programada);
CREATE INDEX IF NOT EXISTS idx_citas_estado_logistico ON citas_recogida (estado_logistico);
