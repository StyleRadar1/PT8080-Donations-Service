-- =============================================================================
-- V2__align_domain_and_tables.sql
-- StyleRadar - Paquete 5: ms-segunda-vida-donaciones
-- Transición y alineación arquitectónica al modelo de economía circular y donaciones
-- Elimina tablas legacy de V1 y crea el esquema relacional normalizado con BIGINT
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Limpieza de tablas preliminares de V1 (en caso de haber sido desplegadas)
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS citas_recogida CASCADE;
DROP TABLE IF EXISTS solicitudes_donacion CASCADE;
DROP TABLE IF EXISTS articulos_segunda_mano CASCADE;

-- -----------------------------------------------------------------------------
-- 2. Tabla: publicaciones_segunda_mano (Catálogo de Reventa C2C)
-- -----------------------------------------------------------------------------
CREATE TABLE publicaciones_segunda_mano (
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

CREATE INDEX idx_pub_usuario ON publicaciones_segunda_mano (usuario_id);
CREATE INDEX idx_pub_estado ON publicaciones_segunda_mano (estado);
CREATE INDEX idx_pub_tipo ON publicaciones_segunda_mano (tipo_prenda);
CREATE INDEX idx_pub_fecha ON publicaciones_segunda_mano (fecha_publicacion DESC);

-- -----------------------------------------------------------------------------
-- 3. Tabla: fotos_publicacion (Fotografías asociadas, máximo 4 por publicación)
-- -----------------------------------------------------------------------------
CREATE TABLE fotos_publicacion (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    publicacion_id BIGINT NOT NULL REFERENCES publicaciones_segunda_mano(id) ON DELETE CASCADE,
    url_foto VARCHAR(512) NOT NULL,
    orden SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_fotos_publicacion ON fotos_publicacion (publicacion_id);

-- -----------------------------------------------------------------------------
-- 4. Tabla: solicitudes_donacion (Postulación benéfica y logística a fundaciones)
-- -----------------------------------------------------------------------------
CREATE TABLE solicitudes_donacion (
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

CREATE INDEX idx_sol_donante ON solicitudes_donacion (donante_id);
CREATE INDEX idx_sol_fundacion ON solicitudes_donacion (fundacion_id);
CREATE INDEX idx_sol_publicacion ON solicitudes_donacion (publicacion_id);
CREATE INDEX idx_sol_estado ON solicitudes_donacion (estado);
CREATE INDEX idx_sol_fecha ON solicitudes_donacion (fecha_solicitud DESC);
