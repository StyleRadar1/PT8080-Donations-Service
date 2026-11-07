-- =============================================================================
-- V1__init_schema.sql
-- StyleRadar - Paquete 5: ms-segunda-vida-donaciones
-- Esquema relacional oficial para PostgreSQL 16
-- Tablas: publicaciones_segunda_mano, fotos_publicacion, solicitudes_donacion
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Tabla: publicaciones_segunda_mano (Catálogo de Reventa C2C)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS publicaciones_segunda_mano (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    nombre_prenda VARCHAR(120) NOT NULL,
    descripcion VARCHAR(1000),
    tipo_prenda VARCHAR(30) NOT NULL,
    marca VARCHAR(80) NOT NULL,
    color VARCHAR(50) NOT NULL,
    estilo VARCHAR(30) NOT NULL,
    talla VARCHAR(20) NOT NULL,
    estado_conservacion VARCHAR(30) NOT NULL,
    precio NUMERIC(12, 2) NOT NULL CHECK (precio >= 0),
    estado VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    fecha_publicacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_pub_usuario ON publicaciones_segunda_mano (usuario_id);
CREATE INDEX IF NOT EXISTS idx_pub_estado ON publicaciones_segunda_mano (estado);
CREATE INDEX IF NOT EXISTS idx_pub_tipo ON publicaciones_segunda_mano (tipo_prenda);
CREATE INDEX IF NOT EXISTS idx_pub_fecha ON publicaciones_segunda_mano (fecha_publicacion DESC);

-- -----------------------------------------------------------------------------
-- 2. Tabla: fotos_publicacion (Fotografías asociadas, máximo 4 por publicación)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS fotos_publicacion (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    publicacion_id BIGINT NOT NULL REFERENCES publicaciones_segunda_mano(id) ON DELETE CASCADE,
    url_foto VARCHAR(512) NOT NULL,
    orden SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_fotos_publicacion ON fotos_publicacion (publicacion_id);

-- -----------------------------------------------------------------------------
-- 3. Tabla: solicitudes_donacion (Postulación benéfica y logística a fundaciones)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS solicitudes_donacion (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    publicacion_id BIGINT NOT NULL REFERENCES publicaciones_segunda_mano(id),
    donante_id BIGINT NOT NULL,
    fundacion_id BIGINT NOT NULL,
    mensaje VARCHAR(500),
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    motivo_rechazo VARCHAR(500),
    punto_entrega VARCHAR(255),
    franja_inicio TIMESTAMP WITH TIME ZONE,
    franja_fin TIMESTAMP WITH TIME ZONE,
    fecha_solicitud TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_decision TIMESTAMP WITH TIME ZONE,
    fecha_recepcion TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_sol_publicacion ON solicitudes_donacion (publicacion_id);
CREATE INDEX IF NOT EXISTS idx_sol_donante ON solicitudes_donacion (donante_id);
CREATE INDEX IF NOT EXISTS idx_sol_fundacion ON solicitudes_donacion (fundacion_id);
CREATE INDEX IF NOT EXISTS idx_sol_estado ON solicitudes_donacion (estado);
