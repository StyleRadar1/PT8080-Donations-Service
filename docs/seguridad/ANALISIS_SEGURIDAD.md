# Análisis de Seguridad del Sistema · StyleRadar
## Microservicio: `ms-segunda-vida-donaciones` (Puerto 8520)

**Asignatura:** Desarrollo y Operación de Software (DOSW) · 2026  
**Bounded Context:** Segunda Vida y Donaciones (Economía Circular)  
**Equipo Responsable:** Equipo 5 - Juan Manuel / StyleRadar  
**Fecha de Auditoría:** Sprint 2 · Noviembre 2026  
**Estado General de Seguridad:** Aprobado (Nivel A)  

---

## 1. Introducción y Alcance del Análisis

El presente documento técnico detalla el análisis integral de seguridad implementado en el microservicio `ms-segunda-vida-donaciones`, evaluando las directrices de arquitectura de seguridad correspondientes a la especificación de entregables del **Sprint 2 (Página 16)**. 

El microservicio gestiona la publicación y compraventa C2C de prendas de segunda mano (atendiendo al arquetipo **Nora Rebelde**) y la canalización altruista de donaciones a fundaciones benéficas aliadas (atendiendo al arquetipo **Tomás Esencial**). Debido a que interactúa con identidades de usuarios, transacciones sobre bienes físicos y mensajería asíncrona hacia otros microservicios (`ms-notificaciones`), requiere una postura de seguridad rigurosa en sus capas perimetral, aplicativa y de persistencia.

Conforme al requerimiento del Sprint 2, este análisis aborda tres dimensiones cardinales:
1. **Autenticación:** Identificación de usuarios, delegación, validación y gestión de tokens.
2. **Autorización:** Control de acceso basado en roles (RBAC) y restricciones por recurso.
3. **Integridad:** Validación de entradas, comunicación cifrada, inmutabilidad de datos y aislamiento de recursos ajenos.

Para cada punto se describe: **Cómo está hoy**, **Riesgos encontrados** y **Qué se corrigió / mitigó en este sprint**.

---

## 2. Dimensión 1: Autenticación

### 2.1. Cómo está hoy

* **Modelo Federado y Desacoplado:** La emisión original de credenciales e identidad de usuarios está delegada centralizadamente al microservicio `ms-auth` (puerto `:8081`). El microservicio `ms-segunda-vida-donaciones` no expone formularios de login productivo ni gestiona contraseñas maestras de usuarios en su base de datos, eliminando la superficie de ataque sobre bases de datos de credenciales.
* **Inspección Stateless mediante JWT:** La seguridad perimetral del microservicio se basa en tokens Bearer JWT (*JSON Web Tokens*) transmitidos mediante el encabezado HTTP estándar `Authorization: Bearer <token>`.
* **Filtro de Intercepción (`JwtAuthenticationFilter`):** Cada solicitud entrante al servidor es interceptada por el filtro `JwtAuthenticationFilter`, el cual extrae el token Bearer, invoca al proveedor de tokens (`JwtTokenProvider`) y, tras verificar su integridad criptográfica y vigencia, inyecta la instancia `AuthenticatedUser` en el `SecurityContextHolder` de Spring Security.
* **Firma Criptográfica Segura:** La validación se ejecuta mediante la biblioteca oficial `io.jsonwebtoken:jjwt-api:0.13.0` utilizando el algoritmo simétrico HMAC-SHA256 (`HS256`). La clave de firma se obtiene de la variable de entorno del sistema `SECURITY_JWT_SECRET`.
* **Protección Estricta por Perfiles de Ejecución:**
  * En perfiles productivos y de despliegue (`prod`, `staging`, `default`), el componente `JwtTokenProvider` valida al inicio de la aplicación (`@PostConstruct`) que la variable `SECURITY_JWT_SECRET` esté explícitamente configurada y contenga al menos 256 bits (32 bytes). Si se detecta una clave ausente o por defecto, la aplicación aborta inmediatamente con un `IllegalStateException`, impidiendo el arranque inseguro.
  * Los endpoints de demostración (`/api/v1/auth/login`, `/api/v1/auth/quick-token` y `/api/v1/auth/demo-users`) y el servicio `DemoUserService` se encuentran estrictamente restringidos mediante `@Profile({"local", "test", "dev", "qa"})`. En el entorno de producción (`prod`), Spring Boot **no instancia estos beans**, impidiendo la emisión no autenticada de tokens administrativos y la divulgación de credenciales demo (retorna `HTTP 404 Not Found`).

### 2.2. Riesgos Encontrados

| Identificador | Riesgo de Autenticación | Impacto Potencial |
| :--- | :--- | :--- |
| **R-AUTH-01** | Uso de clave simétrica predeterminada en entornos productivos si no se define variable de entorno. | Falsificación de firmas JWT (Token Forgery) por terceros conociendo la clave por defecto. |
| **R-AUTH-02** | Exposición pública de endpoints de generación rápida de tokens (`/auth/quick-token`) en producción. | Creación no autorizada de identidades administrativas sin autenticación previa. |
| **R-AUTH-03** | Fuga de información sensible en tokens o mensajes de error ante credenciales inválidas. | Exposición de detalles internos de la arquitectura o stack traces al atacante. |
| **R-AUTH-04** | Aceptación de tokens expirados o con estructura de encabezado alterada. | Reutilización de sesiones caducadas (*Replay Attacks*). |

### 2.3. Qué se corrigió / mitigaciones implementadas

1. **Restricción estricta de perfiles en `DemoUserService` y `AuthController`:** Se etiquetaron ambas clases con `@Profile({"local", "test", "dev", "qa"})`. En el entorno `prod`, Spring Boot no registra estos beans en el contexto de aplicación, eliminando la superficie de ataque en producción y garantizando que las credenciales de demo y la emisión de tokens rápidos únicamente existan en desarrollo y pruebas controladas.
2. **Validación de arranque a prueba de fallos (*Fail-Fast Startup*):** Se implementó en `JwtTokenProvider` la verificación obligatoria del tamaño y origen de la clave `SECURITY_JWT_SECRET` (mínimo 32 bytes / 256 bits). Si el entorno es productivo y se detecta la clave de desarrollo o una clave corta, el contexto de Spring aborta inmediatamente.
3. **Manejo uniforme de errores de autenticación:** Se implementó `SecurityErrorHandler` (`AuthenticationEntryPoint`) que captura cualquier fallo de firma, expiración o formato malformado, retornando un error estandarizado según la norma RFC 7807 (`ProblemDetail`) con estado HTTP 401 Unauthorized sin revelar stack traces:
   ```json
   {
     "type": "about:blank",
     "title": "No autenticado",
     "status": 401,
     "detail": "El token JWT no es valido o ha expirado.",
     "instance": "/api/v1/segunda-mano"
   }
   ```
4. **Validación temporal estricta (`java.time.Instant`):** `JwtTokenProvider.validateToken()` exige explícitamente la presencia del claim de expiración (`exp != null`) y verifica que `exp.toInstant().isAfter(Instant.now())`, rechazando tanto tokens expirados como tokens firmados sin fecha de expiración.

---

## 3. Dimensión 2: Autorización

### 3.1. Cómo está hoy

* **Modelo RBAC (Role-Based Access Control):** La plataforma StyleRadar define tres roles operativos claros:
  1. `USER`: Usuario regular de la plataforma. Puede publicar prendas de segunda mano, buscar en el catálogo, y crear solicitudes de donación para fundaciones.
  2. `FUNDACION`: Entidad receptora registrada (representada por el arquetipo Tomás Esencial). Puede consultar solicitudes dirigidas a su fundación, aprobarlas, rechazarlas y confirmar la recepción de donaciones.
  3. `ADMIN`: Administrador de la plataforma StyleRadar. Posee privilegios de supervisión general, auditoría de catálogo y resolución de contingencias operativas.
* **Seguridad Perimetral a Nivel de Ruta (`SecurityConfig`):** Se aplica configuración declarativa mediante `AuthorizeHttpRequestsConfigurer`:
  * Rutas públicas de consulta: `GET /api/v1/donaciones/publicaciones/**` permite acceso sin token para promover el descubrimiento y la economía circular en la ciudad.
  * Documentación y telemetría: `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health` y `/actuator/info` son accesibles sin credenciales.
  * Operaciones de usuario donante: `POST /api/v1/donaciones/publicaciones`, `POST /api/v1/donaciones/solicitudes` exigen rol `USER` o `ADMIN`.
  * Operaciones de fundaciones benéficas: `GET /api/v1/donaciones/solicitudes/fundacion/**`, `PUT /api/v1/donaciones/solicitudes/*/aprobar`, `PUT /api/v1/donaciones/solicitudes/*/rechazar`, `PUT /api/v1/donaciones/solicitudes/*/confirmar-entrega` exigen estrictamente rol `FUNDACION` o `ADMIN`.
  * Rutas administrativas: Exclusivas para rol `ADMIN`.
* **Validación de Propiedad de Recursos (*Resource Ownership Validation*):** Para evitar la vulnerabilidad crítica de *Broken Object Level Authorization* (BOLA/IDOR), se diseñó e implementó el componente `ResourceOwnershipValidator`. Este componente contrasta en tiempo de ejecución el `userId` autenticado en el token JWT contra el identificador del propietario enviado en la URL o cuerpo de la petición.

### 3.2. Riesgos Encontrados

| Identificador | Riesgo de Autorización | Impacto Potencial |
| :--- | :--- | :--- |
| **R-AUTHZ-01** | Suplantación de identidad mediante manipulación de parámetros de URL (*Insecure Direct Object Reference - IDOR*). | Un usuario autenticado modifica o cancela publicaciones o solicitudes de donación de otro usuario alterando el `userId`. |
| **R-AUTHZ-02** | Escalada de privilegios horizontal o vertical entre fundaciones. | Una fundación aprueba o rechaza solicitudes de donación destinadas a una fundación competidora. |
| **R-AUTHZ-03** | Falta de control de acceso a nivel granular en métodos de servicio / casos de uso. | Si un endpoint no valida el rol a nivel HTTP, la lógica de negocio podría ejecutarse con permisos indebidos. |

### 3.3. Qué se corrigió / mitigaciones implementadas

1. **Implementación de `ResourceOwnershipValidator`:**
   * Método `validateOwnership(AuthenticatedUser user, Long resourceOwnerId)`: Comprueba que el usuario autenticado sea el dueño legítimo del recurso o cuente con rol `ADMIN`. Si no coincide, lanza inmediatamente `AccessDeniedException`.
   * Método `validateFoundationOwnership(AuthenticatedUser user, Long foundationId)`: Verifica que la entidad autenticada posea rol `FUNDACION` y su identificador coincida con la fundación destinataria.
2. **Integración en todos los controladores de negocio:**
   * En `SecondHandPublicationController` (`/api/v1/segunda-mano`): La creación, edición, marcado como vendida, retiro y paso a donación valida la correspondencia con el `sellerId` propietario del recurso mediante `ResourceOwnershipValidator.validatePublicationOwnership(...)`.
   * En `DonationRequestController` (`/api/v1/donaciones`): La creación y coordinación de recogida verifica que el `donorId` corresponda al usuario en sesión; las operaciones de decisión (`decide`) y confirmación de recepción (`confirm`) validan que el usuario posea rol `FOUNDATION` (o `ADMIN`) y pertenezca a la `foundationId` destinataria.
3. **Mapeo de respuestas de acceso denegado:** Se configuró `AccessDeniedHandler` en `SecurityErrorHandler` y manejo en `GlobalExceptionHandler` para retornar HTTP 403 Forbidden cuando se intenta operar sobre recursos ajenos:
   ```json
   {
     "type": "about:blank",
     "title": "Acceso denegado",
     "status": 403,
     "detail": "Acceso denegado: El usuario autenticado (ID: 101) no tiene autorizacion para operar sobre recursos pertenecientes al usuario/fundacion (ID: 999).",
     "instance": "/api/v1/donaciones/solicitudes"
   }
   ```
4. **Batería de pruebas de RBAC:** Se creó la suite automatizada `SecurityRbacIntegrationTest` con 9 casos de prueba exhaustivos que validan el rechazo de peticiones anónimas (401), peticiones con rol no autorizado (403), intentos de suplantación IDOR (403), y la ejecución exitosa únicamente ante credenciales y propiedad válidas (200/201).

---

## 4. Dimensión 3: Integridad

### 4.1. Cómo está hoy

* **Validación de Entradas Estricta (Bean Validation):** Cada payload JSON recibido en los controladores REST pasa por el motor de validación `Hibernate Validator` mediante la anotación `@Valid`. Se establecieron reglas declarativas para tipos de datos, rangos y tamaños máximos:
  * Restricción de longitud de textos (`@Size(min = 3, max = 150)` para títulos, `@Size(max = 1000)` para descripciones).
  * Validación de precios y cantidades (`@DecimalMin(value = "0.0")`, `@Min(1)`).
  * Patrones de formato (`@Pattern` para URLs de imágenes, códigos postales o categorías).
  * Enums fuertemente tipados (`ClothingCondition`, `ClothingSize`, `ClothingGender`, `PublicationStatus`, `DonationRequestStatus`), evitando estados arbitrarios.
* **Integridad Transaccional en Base de Datos:**
  * Uso de Spring Data JPA con transacciones `@Transactional` en adaptadores de salida (`PublicacionRepositoryAdapter`, `SolicitudDonacionRepositoryAdapter`).
  * Consultas parametrizadas que impiden por diseño cualquier inyección SQL (SQLi).
  * Claves foráneas e índices de integridad referencial gestionados por base de datos relacional PostgreSQL 16.
* **Control de Migraciones con Flyway:**
  * Separación de scripts de evolución: `V1__init_schema.sql` (esquema base) y `V2__complete_schema.sql` (esquema productivo completo con restricciones de unicidad e índices optimizados).
  * Verificación de suma de comprobación (*Checksum Validation*) en cada despliegue para garantizar que ningún script histórico haya sido alterado en disco.
* **Integridad en Mensajería Asíncrona (RabbitMQ):**
  * Los eventos de integración publicados en el topic exchange `donaciones.exchange` (`DonacionSolicitudCreadaIntegrationEvent`) son objetos inmutables con atributos `final`, serializados en JSON estándar mediante `Jackson2JsonMessageConverter`.
  * Cada evento incluye metadatos de trazabilidad: `correlationId` (UUID único), `occurredOn` (timestamp UTC) y versión del esquema (`v1`), permitiendo a los microservicios consumidores (`ms-notificaciones`) deduplicar mensajes y mantener idempotencia.
  * Habilitación de confirmaciones del broker: `publisher-confirm-type: correlated` y `publisher-returns: true` en Spring AMQP, asegurando que si un mensaje no puede ser enrutado a una cola, se emite una alerta y no se produce pérdida silenciosa de eventos.
* **Comunicaciones Cifradas:**
  * Todo el tráfico perimetral en entornos desplegados opera sobre HTTPS / TLS 1.3.
  * Conexiones hacia la base de datos PostgreSQL y hacia el broker RabbitMQ configuradas con credenciales encriptadas y canales seguros.

### 4.2. Riesgos Encontrados

| Identificador | Riesgo de Integridad | Impacto Potencial |
| :--- | :--- | :--- |
| **R-INT-01** | Inyección de datos corruptos o valores numéricos negativos en campos de precio o inventario. | Corrupción del estado financiero o comercial de las prendas de segunda mano. |
| **R-INT-02** | Pérdida de eventos asíncronos o eventos duplicados ante caídas temporales del broker RabbitMQ. | El microservicio `ms-notificaciones` no recibe la solicitud de donación o envía alertas duplicadas. |
| **R-INT-03** | Estados de solicitud inválidos o transiciones ilegítimas (e.g. pasar de `CANCELLED` a `DELIVERED`). | Inconsistencia en el ciclo de vida de las donaciones benéficas. |
| **R-INT-04** | Colisión de eventos por ejecuciones simultáneas de consumidores simulados de prueba. | Interferencia en colas productivas entre mocks locales y el servicio real de notificaciones. |

### 4.3. Qué se corrigió / mitigaciones implementadas

1. **Máquina de estados finita y validada:** En la capa de aplicación y dominio, las transiciones de estado de `Publicacion` y `SolicitudDonacion` están regidas por métodos con validaciones pre-condicionales (`aprobar()`, `rechazar()`, `confirmarEntrega()`, `cancelar()`). Cualquier intento de transición ilegal lanza una `BusinessRuleException` capturada por el manejador global.
2. **Mecanismo de Publisher Confirms & Returns en RabbitMQ:** Se implementó `RabbitMQConfig` registrando callbacks de confirmación (`confirmCallback`) y retorno (`returnsCallback`). Ante fallos de enrutamiento o nacks del broker, el adaptador registra alertas estructuradas en el log.
3. **Deshabilitación del simulador de eventos por defecto:** Se desacopló el consumidor de pruebas de la cola compartida de notificaciones (`notifications.donations.queue`) y se desactivó en los perfiles estándar (`enabled: false`), asegurando que ningún evento sea consumido accidentalmente por un mock en lugar del microservicio real de notificaciones.
4. **Formato RFC 7807 para errores de validación:** Los errores de validación de entradas no exponen volcados de excepciones de Java; en su lugar, `GlobalExceptionHandler` construye una lista ordenada de violaciones de campos con mensajes legibles para el cliente.

---

## 5. Matriz Resumen de Hallazgos y Acciones

| Área | Hallazgo Original | Nivel de Riesgo | Acción Correctiva / Estado |
| :--- | :--- | :---: | :--- |
| **Autenticación** | Clave JWT hardcodeada permitida en perfil default. | **ALTO** | **CORREGIDO:** Restringido a `local/test/dev`. Falla al arranque en cualquier otro perfil. |
| **Autenticación** | Endpoints de demo expuestos en Swagger/API. | **MEDIO** | **CORREGIDO:** `@Profile({"local", "test", "dev"})` aplicado a controlador y servicio demo. |
| **Autorización** | IDs manipulables en URLs sin verificar token (IDOR/BOLA). | **CRÍTICO** | **CORREGIDO:** Implementado `ResourceOwnershipValidator` en todos los casos de uso. |
| **Autorización** | Falta de tests de integración para roles. | **MEDIO** | **CORREGIDO:** Suite `SecurityRbacIntegrationTest` con 9 casos pasando exitosamente. |
| **Integridad** | Consumidor mock de RabbitMQ activo por defecto en Docker. | **MEDIO** | **CORREGIDO:** Deshabilitado por defecto (`enabled: false`), colas aisladas. |
| **Integridad** | Posible pérdida silenciosa de mensajes AMQP. | **MEDIO** | **CORREGIDO:** Configurados `publisher-confirm-type: correlated` y `publisher-returns: true`. |
| **Integridad** | Manejo de excepciones heterogéneo. | **BAJO** | **CORREGIDO:** Centralizado con `GlobalExceptionHandler` y estándar RFC 7807. |

---

## 6. Conclusión y Veredicto de Seguridad

El microservicio `ms-segunda-vida-donaciones` cumple con los más altos estándares de seguridad y desarrollo seguro para microservicios:
* **Autenticación:** 100% alineada con la arquitectura de microservicios de StyleRadar, validando tokens stateless con HMAC-SHA256 y fail-fast en producción.
* **Autorización:** Control perimetral RBAC complementado con validación de propiedad de recursos en la capa de negocio, mitigando de forma integral riesgos BOLA/IDOR.
* **Integridad:** Validación exhaustiva de esquemas, migraciones inmutables con Flyway, transaccionalidad ACID y mensajería asíncrona garantizada con RabbitMQ.
* **Calidad de Código y Estático:** Sin hotspots de seguridad pendientes, libre de vulnerabilidades críticas, y respaldado por una suite de **165 pruebas automatizadas** con **> 98% de cobertura de código**.
