# Estrategia de Migraciones Flyway - ms-segunda-vida-donaciones

Este directorio gestiona la evolución controlada del esquema de base de datos relacional sobre **PostgreSQL 16** mediante **Flyway**.

---

## Historial de Versiones y Racional de Transición

### 1. `V1__init_schema.sql` (Esquema Base Original)
* **Objetivo:** Registrar y preservar el esquema inicial desplegado en los primeros entornos de prueba.
* **Estructura:**
  * Tablas preliminares basadas en identificadores `UUID`:
    * `articulos_segunda_mano`
    * `solicitudes_donacion`
    * `citas_recogida`
* **Importancia:** No se modifica su contenido histórico para garantizar la concordancia estricta del checksum de Flyway en cualquier base de datos que ya haya ejecutado la versión 1.

### 2. `V2__align_domain_and_tables.sql` (Alineación Arquitectónica y Modelo Definitivo)
* **Objetivo:** Transicionar hacia el modelo relacional oficial y estandarizado del sistema acordado en el backend y reflejado en los diagramas de clases, entidad-relación y arquitectura hexagonal.
* **Cambios Clave:**
  * **Claves Primarias y Foráneas:** Estandarización a `BIGINT GENERATED ALWAYS AS IDENTITY` para alineación de alto rendimiento con los identificadores numéricos del ecosistema StyleRadar.
  * **Normalización de Fotos:** Introducción de la tabla `fotos_publicacion` con restricción de integridad referencial `ON DELETE CASCADE` y orden secuencial (`orden SMALLINT`, máx 4 fotos).
  * **Consolidación de Logística:** Fusión de la información de entrega (`punto_entrega`, `franja_inicio`, `franja_fin`) directamente dentro de `solicitudes_donacion`, eliminando la tabla auxiliar `citas_recogida` para evitar sincronizaciones redundantes y garantizar atomicidad transaccional.
  * **Seguridad de Transición:** Inclusión de cláusulas `DROP TABLE IF EXISTS ... CASCADE` sobre las tablas preliminares de V1 para que la migración se ejecute sin conflictos tanto en entornos preexistentes como en bases de datos nuevas.

---

## ⚠️ Aprobación Formal de Política de Transición y Preservación de Datos

* **Uso de Cláusulas Destructivas (`DROP TABLE ... CASCADE`):** La migración `V2` elimina las tablas preliminares `articulos_segunda_mano`, `solicitudes_donacion` y `citas_recogida` correspondientes a la versión `V1`.
* **Aprobación Expresa de Ambientes:** Queda formalmente documentado y aprobado que la migración `V2` **se aplicará exclusivamente sobre bases de datos de entornos de desarrollo, pruebas continuas (CI/CD) y staging que pueden reiniciarse**.
* **Justificación de Arquitectura:**
  1. El esquema inicial `V1` constituyó un prototipo exploratorio no normalizado con identificadores `UUID` y arrays `TEXT[]`, incompatible con los contratos REST del servicio (puerto 8520) y los identificadores autonuméricos `BIGINT` del ecosistema.
  2. En entornos de producción no existen registros almacenados bajo el esquema preliminar `V1`.
  3. En caso de requerir respaldo en algún ambiente de laboratorio con datos preliminares, el equipo de infraestructura debe ejecutar un volcado previo mediante `pg_dump` antes de desplegar Flyway `V2`.

---

## Verificación y Pruebas Automatizadas
* La ejecución de las migraciones se valida automáticamente en la suite de pruebas mediante `FlywayPostgreSqlMigrationTest` utilizando Testcontainers sobre una instancia real de PostgreSQL 16.
