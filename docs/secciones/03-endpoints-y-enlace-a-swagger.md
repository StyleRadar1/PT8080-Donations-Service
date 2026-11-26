[← Anterior: 2. Arquitectura y Diagramas](./02-arquitectura-hexagonal-y-diagramas.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 4. Eventos RabbitMQ →](./04-eventos-que-publica-y-consume.md)

# 3. Endpoints y Enlace a Swagger

El microservicio se encuentra desplegado y activo en **Microsoft Azure** dentro del Resource Group dedicado **`rg-styleradar`** (`Canada Central` / `East US 2`).

## 3.1 Matriz Oficial de Enlaces por Entorno (PROD, QA, LOCAL)

| Entorno | Estado | Propósito | URL Base del Servicio | Swagger OpenAPI UI | Health Check (`/api/v1/health`) |
| :--- | :---: | :--- | :--- | :--- | :--- |
| **Producción (`PROD`)** | 🟢 **Active** | Entorno productivo en Azure Cloud con perfil endurecido | `https://styleradar-donaciones-prod-jmunar.azurewebsites.net` | [Swagger UI PROD](https://styleradar-donaciones-prod-jmunar.azurewebsites.net/api/v1/swagger-ui/index.html) | [`/api/v1/health`](https://styleradar-donaciones-prod-jmunar.azurewebsites.net/api/v1/health) |
| **Calidad (`QA`)** | 🟢 **Active** | Entorno de pruebas integradas y staging en Azure Cloud | `https://styleradar-donaciones-qa-jmunar.azurewebsites.net` | [Swagger UI QA](https://styleradar-donaciones-qa-jmunar.azurewebsites.net/api/v1/swagger-ui/index.html) | [`/api/v1/health`](https://styleradar-donaciones-qa-jmunar.azurewebsites.net/api/v1/health) |
| **Desarrollo Local (`DEV`)** | 🟢 **Active** | Entorno local de desarrollo (H2 o Docker Compose) | `http://localhost:8520` | [Swagger UI Local](http://localhost:8520/api/v1/swagger-ui.html) | [`/api/v1/health`](http://localhost:8520/api/v1/health) |

* **Especificación OpenAPI (JSON Contrato):** [`/api/v1/v3/api-docs`](https://styleradar-donaciones-qa-jmunar.azurewebsites.net/api/v1/v3/api-docs)
* **Colección Postman & Newman (Suite completa de todos los endpoints y códigos HTTP):** [`docs/postman/`](../postman/) (`StyleRadar-Donations-Service.postman_collection.json`, entornos `Local`/`QA` y simulador `mock-tiendas.js`)

## 3.2 Tabla Canónica de Endpoints REST Expuestos

Todos los endpoints responden bajo el estándar **RFC 7807 (`application/problem+json`)** y fichas **CADche v8** (`type: https://api.styleradar.com/errors/{code}`) incluyendo `code`, lista estructurada `errors[]` (`field`, `message`) y `timestamp`, con esquemas y ejemplos detallados en Swagger UI:

| Tag Swagger | Método | Endpoint Canónico | RF Asociado | Descripción | Rol Requerido | Códigos HTTP |
| :--- | :---: | :--- | :---: | :--- | :--- | :---: |
| **Health Diagnostics** | `GET` | `/api/v1/health` | Diagnóstico | Estado UP, versión, puerto y hash de commit | Público | `200` |
| **Demo Auth** | `POST` | `/api/v1/auth/login` | Seguridad | Iniciar sesión por email y password en Swagger | local/dev/test | `200`, `400`, `500` |
| **Demo Auth** | `GET` | `/api/v1/auth/quick-token` | Seguridad | Generar token JWT rápido por rol (`USER`, `FOUNDATION`, `ADMIN`) | local/dev/test | `200`, `400`, `500` |
| **Demo Auth** | `GET` | `/api/v1/auth/demo-users` | Seguridad | Catálogo de identidades demo precargadas | local/dev/test | `200`, `500` |
| **Photo Storage** | `POST` | `/api/v1/second-hand/photos` | Azure Storage | Subir imagen (JPEG/PNG/WEBP, máximo de 5 MB) a Azure Blob Storage | `USER`, `STORE`, `FOUNDATION`, `ADMIN` | `201`, `400`, `401`, `403`, `500` |
| **Second-Hand Publications** | `POST` | `/api/v1/second-hand/publications` | `SR-RF-33` | Publicar prenda para reventa C2C (>0 COP) o donación (0 COP), incluyendo atributos, `ConservationStatus` y precio | `USER`, `STORE`, `ADMIN` | `201`, `400`, `401`, `403`, `500` |
| **Second-Hand Publications** | `GET` | `/api/v1/second-hand/publications` | `SR-RF-36` | Explorar y buscar prendas (`?search=` / `?searchTerm=`, categoría, color, talla, localidad, precio, `AVAILABLE` por defecto) | Público | `200`, `400`, `500` |
| **Second-Hand Publications** | `GET` | `/api/v1/second-hand/publications/{id}` | `SR-RF-36` | Consultar detalle de publicación por ID | Público | `200`, `404`, `500` |
| **Second-Hand Publications** | `GET` | `/api/v1/second-hand/publications/users/{userId}` | `SR-RF-110` | Consultar y gestionar listado de publicaciones propias del usuario | Propietario, `ADMIN` | `200`, `401`, `403`, `500` |
| **Second-Hand Publications** | `PUT` | `/api/v1/second-hand/publications/{id}` | `SR-RF-37` | Editar prenda (precio, fotos, talla, descripción `@NotBlank`) en estado `AVAILABLE` | Propietario, `ADMIN` | `200`, `400`, `401`, `403`, `404`, `409`, `500` |
| **Second-Hand Publications** | `PATCH` | `/api/v1/second-hand/publications/{id}/sale?userId={userId}&compradorId={compradorId}` | `SR-RF-38` | Marcar como vendida (`SOLD`). Requiere los dos parámetros de consulta obligatorios `userId` y `compradorId` (ejemplo: `PATCH /api/v1/second-hand/publications/10/sale?userId=101&compradorId=102`) y rechaza `interesadosIds` arbitrarios enviados desde cliente | Propietario, `ADMIN` | `200`, `400`, `401`, `403`, `404`, `409`, `500` |
| **Second-Hand Publications** | `DELETE` | `/api/v1/second-hand/publications/{id}` | `SR-RF-39` | Retirar publicación activa de la vitrina (bloquea `SOLD`, `IN_DONATION`, `DONATED`) | Propietario, `ADMIN` | `204`, `401`, `403`, `404`, `409`, `500` |
| **Second-Hand Publications** | `PATCH` | `/api/v1/second-hand/publications/{id}/donation` | `SR-RF-42` | Convertir publicación existente al canal benéfico (`IN_DONATION`, precio `0 COP`) | Propietario, `ADMIN` | `200`, `401`, `403`, `404`, `409`, `500` |
| **Charitable Donations** | `GET` | `/api/v1/donations/foundations` | `SR-RF-45` | Listar fundaciones aliadas activas desde `ms-tiendas` (filtro opcional `?category=`) | `USER`, `STORE`, `FOUNDATION`, `ADMIN` | `200`, `401`, `503` |
| **Charitable Donations** | `GET` | `/api/v1/donations/foundations/representative` | `SR-RF-46` | Obtener la fundación asociada al representante autenticado (`sub` ↔ `representanteId`) | `FOUNDATION`, `ADMIN` | `200`, `401`, `403`, `404`, `503` |
| **Charitable Donations** | `GET` | `/api/v1/donations/foundations/{foundationId}` | `SR-RF-45` | Consultar detalle institucional de una fundación activa en `ms-tiendas` | `USER`, `STORE`, `FOUNDATION`, `ADMIN` | `200`, `400`, `401`, `404`, `503` |
| **Charitable Donations** | `POST` | `/api/v1/donations/claims` | `SR-RF-111` | Reclamar prenda disponible desde la Vitrina Abierta por una fundación con bloqueo optimista (`@Version`) | `FOUNDATION`, `ADMIN` | `201`, `400`, `401`, `403`, `404`, `409`, `503` |
| **Charitable Donations** | `POST` | `/api/v1/donations` | `SR-RF-45` | Crear solicitud de donación: obtiene `donorId` exclusivamente del token JWT (ignorando cualquier `donorId` en el payload) y acepta objeto estructurado `deliveryProposal` (`deliveryMode`/`mode`, `deliveryPoint`/`point`, `startTime`, `endTime`) o campos planos retrocompatibles | `USER`, `STORE`, `ADMIN` | `201`, `400`, `401`, `403`, `404`, `409`, `503` |
| **Charitable Donations** | `GET` | `/api/v1/donations` | `SR-RF-46` | Listar solicitudes contextualizadas por actor autenticado (`?status=` opcional): `ADMIN` obtiene todas las solicitudes de la plataforma, `FOUNDATION` resuelve su fundación verificada vía representante autenticado y `USER`/`STORE` obtienen su historial de donante | `USER`, `STORE`, `FOUNDATION`, `ADMIN` | `200`, `400`, `401`, `404`, `503` |
| **Charitable Donations** | `GET` | `/api/v1/donations/{id}` | `SR-RF-46` | Consultar estado y cita logística de una solicitud (protegido contra BOLA/IDOR) | Donante, Fundación, `ADMIN` | `200`, `401`, `403`, `404`, `500` |
| **Charitable Donations** | `GET` | `/api/v1/donations/donors/{donorId}` | `SR-RF-46` | Historial de solicitudes originadas por el donante | Donante propietario, `ADMIN` | `200`, `401`, `403`, `500` |
| **Charitable Donations** | `GET` | `/api/v1/donations/foundations/{foundationId}/requests` | `SR-RF-46` | Bandeja de solicitudes recibidas por la fundación (`?status=` opcional) | Fundación receptora / Representante, `ADMIN` | `200`, `401`, `403`, `500` |
| **Charitable Donations** | `PATCH` | `/api/v1/donations/{id}/decision` | `SR-RF-47` | Aceptar (`APPROVED`) o rechazar (`REJECTED`, revierte prenda a `AVAILABLE`) | Fundación receptora / Representante, `ADMIN` | `200`, `400`, `401`, `403`, `404`, `409`, `500` |
| **Charitable Donations** | `PATCH` | `/api/v1/donations/{id}/pickup` | `SR-RF-45` / `SR-RF-111` | Transición técnica logística auxiliar para agendar franja horaria futura (`America/Bogota`) y punto de entrega tras aprobación o reclamo (`APPROVED` → `IN_PICKUP`) | Donante, Fundación / Representante, `ADMIN` | `200`, `400`, `401`, `403`, `404`, `409`, `500` |
| **Charitable Donations** | `PATCH` | `/api/v1/donations/{id}/confirmation` | `SR-RF-49` | Confirmar recepción física (`DELIVERED`), marcar prenda `DONATED` y emitir comprobante `SR-DON-XXXX` | Fundación receptora / Representante, `ADMIN` | `200`, `400`, `401`, `403`, `404`, `409`, `500` |

## 3.3 Seguridad RBAC, Integración `ms-tiendas` y Trazabilidad (`BUG-003` / `BUG-021` / `BUG-031`)

* **Separación estricta entre catálogo público y autorización RBAC (`ms-tiendas`):**
  * El catálogo público `/api/v1/foundations` (`FundacionPublicaResponseDTO` en `ms-tiendas`) **no incluye `representativeId`**; por ello `TiendasFoundationCatalogAdapter` únicamente lo utiliza como respaldo de lectura pública para validar si una fundación existe y está activa (`findFoundationById`).
  * Para verificación de propiedad y resolución de representante (`isFoundationRepresentative` y `findFoundationByRepresentativeId`), el adaptador exige exclusivamente canales autenticados que expongan `representativeId`: endpoints internos `/api/v1/internal/foundations/{id}` o `?representativeId={id}` con HTTP Basic Auth, o `/api/v1/foundations/representative` con el token `Bearer` entrante del representante. Si no existe asociación verificable (`representativeId == null`) o falta canal autenticado, la operación falla de forma segura (`404`/`503`) sin recurrir al listado público.
* **Manejo limpio de errores RFC 7807 (`BUG-021`):** `GlobalExceptionHandler` sanitiza mensajes de tipo/formato (`400`, `404`, `405`, `415`) en español claro sin exponer nombres internos de clases Java ni trazas técnicas, adjuntando `correlationId` cuando está presente en MDC.
* **Trazabilidad HTTP y pendientes transversales (`BUG-031`):**
  * `CorrelationIdFilter` (`@Order(Ordered.HIGHEST_PRECEDENCE)`) captura o genera `X-Correlation-Id` y `X-Request-Id`, los carga en MDC (`correlationId`, `requestId`), los devuelve en las cabeceras de respuesta y `TiendasClientConfig` los propaga hacia llamadas HTTP salientes a `ms-tiendas`.
  * **Pendientes transversales fuera del alcance de este microservicio:** (1) generación y propagación consistente de `X-Correlation-Id` / `X-Request-Id` desde el API Gateway y en los controladores de `ms-tiendas`, y (2) propagación de headers de correlación en mensajes AMQP de RabbitMQ y consumidores asíncronos (`ms-notificaciones`, `ms-estadisticas`, `ms-chat`).

---

[← Anterior: 2. Arquitectura y Diagramas](./02-arquitectura-hexagonal-y-diagramas.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 4. Eventos RabbitMQ →](./04-eventos-que-publica-y-consume.md)
