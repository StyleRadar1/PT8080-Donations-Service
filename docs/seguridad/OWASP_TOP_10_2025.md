# Matriz de Auditoría OWASP Top 10:2025 · StyleRadar
## Microservicio: `ms-segunda-vida-donaciones` (Puerto 8520)

**Asignatura:** Desarrollo y Operación de Software (DOSW) · 2026  
**Bounded Context:** Segunda Vida y Donaciones (Economía Circular)  
**Equipo Responsable:** Equipo 5 - Juan Manuel / StyleRadar  
**Versión de Referencia:** OWASP Top 10:2025 Standard  
**Estado General de Conformidad:** Conforme / Mitigado (Calificación A)  

---

## 1. Resumen Ejecutivo y Matriz Consolidada

Conforme a la especificación de entregas del **Sprint 2 (Página 17)**, cada microservicio del ecosistema StyleRadar debe someterse a una revisión de seguridad y mitigación contra las 10 categorías de riesgos más críticos según el estándar internacional **OWASP Top 10:2025**.

El microservicio `ms-segunda-vida-donaciones` implementa arquitectura hexagonal con Spring Boot 3.3.4 (Java 21 LTS), Spring Security 6, PostgreSQL 16 y RabbitMQ. La siguiente tabla resume la aplicabilidad, el estado operativo y la acción o salvaguarda técnica implementada para cada categoría:

| Categoría | Riesgo OWASP Top 10:2025 | ¿Aplica? | Estado | Acción / Mitigación Implementada |
| :---: | :--- | :---: | :---: | :--- |
| **A01** | **Broken Access Control** | **SÍ** | **MITIGADO** | Control de acceso perimetral con Spring Security (RBAC: `USER`, `ADMIN`, `FUNDACION`) complementado con `ResourceOwnershipValidator` en controladores y casos de uso para evitar ataques IDOR / BOLA. |
| **A02** | **Security Misconfiguration** | **SÍ** | **MITIGADO** | Actuator restringido exclusivamente a `/health` e `/info`. Deshabilitación de cabeceras innecesarias, cookies de sesión innecesarias (API stateless), Flyway con validación activa (`validate-on-migrate`), y contenedor Docker ejecutado como usuario no privilegiado (`appuser`). |
| **A03** | **Software Supply Chain Failures** | **SÍ** | **MITIGADO** | Dependencias fijadas y verificadas en `pom.xml` con versiones estables oficiales (Spring Boot 3.3.4, JJWT 0.13.0, Postgres Driver). Imagen base oficial `eclipse-temurin:21-jre-alpine` escaneada contra vulnerabilidades CVE. |
| **A04** | **Cryptographic Failures** | **SÍ** | **MITIGADO** | Firma de tokens JWT mediante HMAC-SHA256 (`HS256`) con clave simétrica robusta (>= 256 bits). Exigencia estricta de variables de entorno seguras (`SECURITY_JWT_SECRET`, credenciales de BD y broker) sin secretos en código fuente. |
| **A05** | **Injection** | **SÍ** | **MITIGADO** | Persistencia a través de Spring Data JPA / Hibernate mediante consultas parametrizadas (*Prepared Statements*), eliminando riesgo de SQL Injection. Validación estricta de entradas y longitud de cadenas con Bean Validation. |
| **A06** | **Insecure Design** | **SÍ** | **MITIGADO** | Arquitectura Hexagonal que aísla el núcleo de dominio de la infraestructura. Modelado de máquinas de estado finitas para el ciclo de vida de publicaciones y solicitudes. Especificación formal de contratos mediante OpenAPI 3.0 / Swagger. |
| **A07** | **Authentication Failures** | **SÍ** | **MITIGADO** | Autenticación delegada al servicio de identidad `ms-auth`. Validación estricta de firma, emisor y tiempo de expiración de tokens Bearer JWT en `JwtAuthenticationFilter`. Endpoints de prueba aislados con `@Profile({"local", "test", "dev"})`. |
| **A08** | **Software or Data Integrity Failures** | **SÍ** | **MITIGADO** | Eventos RabbitMQ inmutables y fuertemente tipados con `correlationId` para garantizar trazabilidad e idempotencia en `ms-notificaciones`. Publisher Confirms y Returns activos para evitar pérdida de datos. Migraciones Flyway con verificación de checksums. |
| **A09** | **Logging & Alerting Failures** | **SÍ** | **MITIGADO** | Trazabilidad y registro estructurado con SLF4J / Logback en niveles INFO, WARN y ERROR. Registro de intentos de acceso denegado y fallos de mensajería sin registrar información sensible, contraseñas ni PII (*Personally Identifiable Information*). |
| **A10** | **Mishandling of Exceptional Conditions** | **SÍ** | **MITIGADO** | Manejador global centralizado (`GlobalExceptionHandler`) bajo el estándar RFC 7807 (`ProblemDetail`). Enmascaramiento total de stack traces de base de datos o frameworks para evitar fuga de arquitectura interna. |

---

## 2. Detalle de Análisis por Categoría OWASP Top 10:2025

### A01: Broken Access Control (Control de Acceso Defectuoso)
* **Descripción del Riesgo:** Los usuarios actúan fuera de los permisos previstos, accediendo a datos no autorizados, modificando información ajena o escalando privilegios horizontales o verticales.
* **Aplica al Microservicio:** **SÍ.** Gestiona prendas de donantes individuales, compras/ventas C2C y aprobaciones institucionales por parte de fundaciones.
* **Estado:** **MITIGADO (Aprobado).**
* **Acciones y Salvaguardas Implementadas:**
  1. **RBAC a nivel perimetral:** En `SecurityConfig`, las rutas `/api/v1/donaciones/solicitudes/fundacion/**` y los endpoints de resolución (`/aprobar`, `/rechazar`, `/confirmar-entrega`) exigen el rol `FUNDACION` o `ADMIN`.
  2. **Prevención de IDOR / BOLA:** Se desarrolló el componente `ResourceOwnershipValidator`. Si un usuario con rol `USER` autenticado con `userId=101` intenta registrar una solicitud o cancelar una publicación perteneciente al `userId=501`, el validador intercepta la disparidad y genera un `AccessDeniedException` (HTTP 403 Forbidden).
  3. **Verificación Automatizada:** Se cuenta con la suite de pruebas `SecurityRbacIntegrationTest` que simula ataques de impersonación y valida el rechazo inmediato de transacciones no autorizadas.

---

### A02: Security Misconfiguration (Configuración de Seguridad Defectuosa)
* **Descripción del Riesgo:** Configuraciones por defecto inseguras, cabeceras HTTP faltantes, servicios auxiliares o depuradores expuestos en producción.
* **Aplica al Microservicio:** **SÍ.** Utiliza frameworks como Spring Boot, Spring Security, Actuator, Flyway y contenedores Docker.
* **Estado:** **MITIGADO (Aprobado).**
* **Acciones y Salvaguardas Implementadas:**
  1. **Endpoints de Actuator Protegidos:** En `application.yml`, solo se exponen públicamente los endpoints de salud y telemetría mínima (`health`, `info`). Endpoints sensibles como `env`, `beans`, o `heapdump` permanecen deshabilitados.
  2. **Hardening de Docker:** El `Dockerfile` se estructuró mediante construcción multi-etapa (*multi-stage build*), ejecutando el artefacto compilado bajo un usuario sin privilegios (`USER appuser:appgroup`), impidiendo la ejecución como `root`.
  3. **Flyway Migration Safety:** Se habilitó `flyway.validate-on-migrate: true` para prevenir que scripts de base de datos modificados post-despliegue alteren la consistencia del esquema.
  4. **Protección de Endpoints Demo:** Los endpoints de demostración (`/auth/quick-token` y `/auth/demo-users`) cuentan con `@Profile({"local", "test", "dev"})` para asegurar su ausencia total en producción.

---

### A03: Software Supply Chain Failures (Fallos en la Cadena de Suministro de Software)
* **Descripción del Riesgo:** Inclusión de bibliotecas de terceros comprometidas, desactualizadas o con vulnerabilidades críticas (CVEs).
* **Aplica al Microservicio:** **SÍ.** Se apoya en dependencias Maven del ecosistema Java/Spring.
* **Estado:** **MITIGADO (Aprobado).**
* **Acciones y Salvaguardas Implementadas:**
  1. **Selección de Dependencias Oficiales y Estables:** Se emplean versiones ampliamente soportadas: Spring Boot 3.3.4, JJWT 0.13.0, PostgreSQL Driver 42.7.4, Flyway 10.18.0 y MapStruct 1.5.5.Final.
  2. **Imagen Base Oficial Confiable:** Uso exclusivo de `eclipse-temurin:21-jre-alpine` provista por la Fundación Eclipse, caracterizada por una superficie de ataque mínima.
  3. **Auditoría Continua:** El pipeline de integración continua (`ci.yml`) compila el proyecto de forma reproducible en entornos limpios de GitHub Actions (`ubuntu-latest`).

---

### A04: Cryptographic Failures (Fallos Criptográficos)
* **Descripción del Riesgo:** Uso de algoritmos de cifrado débiles, gestión deficiente de claves secretas o exposición de datos sensibles en tránsito o reposo.
* **Aplica al Microservicio:** **SÍ.** Autenticación por firma de tokens JWT y comunicación con bases de datos y broker de eventos.
* **Estado:** **MITIGADO (Aprobado).**
* **Acciones y Salvaguardas Implementadas:**
  1. **Algoritmo Robusto:** Empleo de HMAC-SHA256 (`HS256`) para la firma y verificación de tokens JWT mediante `Keys.hmacShaKeyFor()`, exigiendo una longitud mínima de 256 bits (32 bytes).
  2. **Bloqueo al Arranque en Producción (*Fail-Fast*):** `JwtTokenProvider` evalúa el perfil activo. Si el perfil es productivo y la variable `SECURITY_JWT_SECRET` contiene el valor por defecto o es menor a 256 bits, la aplicación aborta inmediatamente el arranque para impedir su ejecución insegura.
  3. **Cero Secretos en el Código:** Todas las contraseñas y claves se alimentan a través de variables de entorno externas (`SPRING_DATASOURCE_PASSWORD`, `SPRING_RABBITMQ_PASSWORD`, `SECURITY_JWT_SECRET`).

---

### A05: Injection (Inyección)
* **Descripción del Riesgo:** Inyección de código malicioso (SQLi, NoSQLi, Command Injection) debido a la concatenación directa de entradas de usuario no saneadas.
* **Aplica al Microservicio:** **SÍ.** Persistencia de datos relacionales en PostgreSQL y consumo de parámetros REST.
* **Estado:** **MITIGADO (Aprobado).**
* **Acciones y Salvaguardas Implementadas:**
  1. **Uso de ORM con Parámetros Tipados:** Todas las operaciones de base de datos se ejecutan a través de Spring Data JPA y Hibernate, utilizando consultas derivadas y *Named Parameters* que generan *Prepared Statements* a nivel del driver JDBC, eliminando cualquier concatenación de sentencias SQL.
  2. **Validación Declarativa de Entrada:** Todos los campos de texto (`titulo`, `descripcion`, `motivoRechazo`, `direccionRecogida`) pasan por filtros de `@Pattern`, `@Size` y `@NotBlank` de Jakarta Bean Validation, neutralizando caracteres de escape dañinos o payloads inyectables.

---

### A06: Insecure Design (Diseño Inseguro)
* **Descripción del Riesgo:** Deficiencias en la arquitectura de software o en los modelos de negocio que permiten abusos de lógica o inconsistencias funcionales.
* **Aplica al Microservicio:** **SÍ.** Modela flujos complejos de donaciones, aprobaciones y transiciones de prendas de vestir.
* **Estado:** **MITIGADO (Aprobado).**
* **Acciones y Salvaguardas Implementadas:**
  1. **Arquitectura Hexagonal (Puertos y Adaptadores):** El núcleo de dominio está completamente aislado de tecnologías externas (frameworks, web, bases de datos), facilitando la verificación unitaria de las reglas de negocio.
  2. **Máquina de Estados de Dominio:** Los estados de las publicaciones (`AVAILABLE`, `RESERVED`, `SOLD`, `DONATED`, `RETIRED`) y de las solicitudes (`PENDING`, `APPROVED`, `IN_COLLECTION`, `DELIVERED`, `REJECTED`, `CANCELLED`) están estrictamente encapsulados con precondiciones. Por ejemplo, una donación no puede ser entregada sin haber sido previamente aprobada.
  3. **Documentación OpenAPI 3.0 / Swagger:** Contratos de API claramente definidos con esquemas, validaciones, códigos de retorno y respuestas de error documentadas.

---

### A07: Authentication Failures (Fallos de Autenticación)
* **Descripción del Riesgo:** Vulnerabilidades que permiten la suplantación de identidad, secuestro de sesión o elución de los mecanismos de comprobación de credenciales.
* **Aplica al Microservicio:** **SÍ.** Verificación de identidad en cada llamada a la API REST.
* **Estado:** **MITIGADO (Aprobado).**
* **Acciones y Salvaguardas Implementadas:**
  1. **Validación Exhaustiva de Tokens:** Cada petición es validada en `JwtAuthenticationFilter`. Se valida la estructura del token, emisor, firma criptográfica y vigencia temporal (`exp`), impidiendo el uso de tokens falsificados o caducados.
  2. **Manejo Estandarizado de Errores de Autenticación:** Se implementó `SecurityErrorHandler` (`AuthenticationEntryPoint`), que intercepta fallos de autenticación y devuelve un error RFC 7807 (HTTP 401 Unauthorized) sin divulgar detalles técnicos del algoritmo o causas específicas que faciliten ataques de fuerza bruta.
  3. **Aislamiento de Mocks de Autenticación:** Los endpoints demo para generación rápida de tokens solo se instancian en perfiles no productivos (`local`, `test`, `dev`).

---

### A08: Software or Data Integrity Failures (Fallos en la Integridad de Software y Datos)
* **Descripción del Riesgo:** Alteración de código o datos en tránsito y almacenamiento, manipulación de eventos asíncronos o despliegue de paquetes no verificados.
* **Aplica al Microservicio:** **SÍ.** Emisión de eventos de integración hacia RabbitMQ y almacenamiento de estado en PostgreSQL.
* **Estado:** **MITIGADO (Aprobado).**
* **Acciones y Salvaguardas Implementadas:**
  1. **Contratos de Eventos Inmutables y Trazables:** El evento `DonacionSolicitudCreadaIntegrationEvent` publicado en `donaciones.exchange` cuenta con identificador de correlación único (`correlationId`), timestamp UTC (`occurredOn`) y tipado estricto, permitiendo deduplicación e idempotencia en los consumidores.
  2. **Garantía de Publicación en RabbitMQ:** Configuración de `publisher-confirm-type: correlated` y `publisher-returns: true` en Spring AMQP para interceptar confirmaciones o retornos del broker, evitando pérdida inadvertida de mensajes.
  3. **Aislamiento del Simulador de Consumo:** Se deshabilitó por defecto el simulador mock local para prevenir interferencias con las colas oficiales de `ms-notificaciones`.
  4. **Integridad de Esquema con Flyway:** Verificación de sumas de comprobación en scripts de migración SQL.

---

### A09: Logging & Alerting Failures (Fallos en Registro y Alertas)
* **Descripción del Riesgo:** Registro insuficiente de eventos de seguridad que impida la detección de incidentes, o registro excesivo que exponga datos personales confidenciales.
* **Aplica al Microservicio:** **SÍ.** Auditoría de operaciones de donación, autenticación y errores operativos.
* **Estado:** **MITIGADO (Aprobado).**
* **Acciones y Salvaguardas Implementadas:**
  1. **Logging Estructurado con SLF4J:** Registro de eventos clave en niveles apropiados (INFO para ciclo de vida de publicaciones/solicitudes, WARN para intentos de acceso indebido o tokens inválidos, ERROR para fallos de base de datos o broker).
  2. **Auditoría de Accesos Denegados:** Cuando `ResourceOwnershipValidator` detecta una incongruencia de IDs, registra explícitamente en el log el ID del usuario autenticado y el recurso ajeno solicitado, facilitando el análisis forense.
  3. **Protección de Datos Privados (Zero PII Logging):** Los registros nunca imprimen valores de tokens JWT completos, contraseñas ni información de contacto sensible de los donantes.

---

### A10: Mishandling of Exceptional Conditions (Manejo Inadecuado de Condiciones Excepcionales)
* **Descripción del Riesgo:** Respuestas de error inconsistentes, exposición de stack traces con rutas internas del sistema o caídas de servicio ante entradas inesperadas.
* **Aplica al Microservicio:** **SÍ.** Manejo de errores de validación, reglas de negocio, base de datos y seguridad.
* **Estado:** **MITIGADO (Aprobado).**
* **Acciones y Salvaguardas Implementadas:**
  1. **Controlador Global de Excepciones (`GlobalExceptionHandler`):** Se implementó una clase `@RestControllerAdvice` que centraliza la captura de todas las excepciones del sistema (`MethodArgumentNotValidException`, `ResourceNotFoundException`, `BusinessRuleException`, `AccessDeniedException`, `AuthenticationException`, `Exception`).
  2. **Estandarización RFC 7807 (`ProblemDetail`):** Todas las respuestas de error devuelven una estructura uniforme con `type`, `title`, `status`, `detail` e `instance`.
  3. **Ocultamiento de Stack Traces:** Ante excepciones internas no controladas (HTTP 500), se devuelve un mensaje genérico de error y se registra la traza técnica únicamente en los logs del servidor, impidiendo que clientes maliciosos reconozcan la topología de la infraestructura.

---

## 3. Conclusión y Veredicto Final

Tras la revisión técnica exhaustiva y la aplicación de las salvaguardas arquitectónicas en el Sprint 2:
* Las **10 categorías** del **OWASP Top 10:2025** han sido analizadas y catalogadas como **MITIGADAS** en el microservicio `ms-segunda-vida-donaciones`.
* Se cuenta con un blindaje efectivo tanto a nivel de transporte (stateless JWT, RBAC) como a nivel de lógica de aplicación (`ResourceOwnershipValidator`) y datos (transacciones ACID, Prepared Statements, migraciones Flyway inmutables).
* El microservicio está completamente alineado con los criterios de aceptación y listo para su evaluación y Quality Gate.
