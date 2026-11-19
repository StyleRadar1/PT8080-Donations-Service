[← Anterior: 3. Arquitectura y Diagramas](./03-arquitectura-diagramas.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 5. Eventos RabbitMQ →](./05-eventos-rabbitmq.md)

# 4. Endpoints y Enlace a Swagger

El microservicio se encuentra desplegado y activo en **Microsoft Azure** dentro del Resource Group dedicado **`rg-styleradar`** (`Canada Central` / `East US 2`).

## 4.1 Matriz Oficial de Enlaces por Entorno (PROD, QA, LOCAL)

| Entorno | Estado | Propósito | URL Base del Servicio | Swagger OpenAPI UI | Health Check (`/health`) |
| :--- | :---: | :--- | :--- | :--- | :--- |
| **Producción (`PROD`)** | 🟢 **Active** | Entorno productivo en Azure Cloud con perfil endurecido | `https://styleradar-donaciones-prod-jmunar.azurewebsites.net` | [Swagger UI PROD](https://styleradar-donaciones-prod-jmunar.azurewebsites.net/api/v1/swagger-ui/index.html) | [`/api/v1/health`](https://styleradar-donaciones-prod-jmunar.azurewebsites.net/api/v1/health) |
| **Calidad (`QA`)** | 🟢 **Active** | Entorno de pruebas integradas y staging en Azure Cloud | `https://styleradar-donaciones-qa-jmunar.azurewebsites.net` | [Swagger UI QA](https://styleradar-donaciones-qa-jmunar.azurewebsites.net/api/v1/swagger-ui/index.html) | [`/api/v1/health`](https://styleradar-donaciones-qa-jmunar.azurewebsites.net/api/v1/health) |
| **Desarrollo Local (`DEV`)** | 🟢 **Active** | Entorno local de desarrollo (H2 o Docker Compose) | `http://localhost:8520` | [Swagger UI Local](http://localhost:8520/api/v1/swagger-ui.html) | [`/api/v1/health`](http://localhost:8520/api/v1/health) |

* **Especificación OpenAPI (JSON Contrato):** [`/api/v1/v3/api-docs`](https://styleradar-donaciones-qa-jmunar.azurewebsites.net/api/v1/v3/api-docs)
* **Colección Postman & Newman (Suite completa de todos los endpoints y códigos HTTP):** [`docs/postman/`](../postman/) (`StyleRadar-Donations-Service.postman_collection.json`, entornos `Local`/`QA` y simulador `mock-tiendas.js`)

## 4.2 Tabla Canónica de Endpoints REST Expuestos

Todos los endpoints responden bajo el estándar **RFC 7807 (`application/problem+json`)** para códigos de error con esquemas y ejemplos detallados en Swagger UI:

| Tag Swagger | Método | Endpoint Canónico | RF Asociado | Descripción | Rol Requerido | Códigos HTTP |
| :--- | :---: | :--- | :---: | :--- | :--- | :---: |
| **Health Diagnostics** | `GET` | `/api/v1/health` | Diagnóstico | Estado UP, versión, puerto y hash de commit | Público | `200` |
| **Demo Auth** | `POST` | `/api/v1/auth/login` | Seguridad | Iniciar sesión por email y password en Swagger | local/dev/test | `200`, `400`, `500` |
| **Demo Auth** | `GET` | `/api/v1/auth/quick-token` | Seguridad | Generar token JWT rápido por rol (`USER`, `FOUNDATION`, `ADMIN`) | local/dev/test | `200`, `400`, `500` |
| **Demo Auth** | `GET` | `/api/v1/auth/demo-users` | Seguridad | Catálogo de identidades demo precargadas | local/dev/test | `200`, `500` |
| **Photo Storage** | `POST` | `/api/v1/second-hand/photos` | Azure Storage | Subir imagen (JPEG/PNG/WEBP, máx 5MB) a Azure Blob Storage | `USER`, `STORE`, `FOUNDATION`, `ADMIN` | `201`, `400`, `401`, `403`, `500` |
| **Second-Hand Publications** | `POST` | `/api/v1/second-hand/publications` | `SR-RF-33` | Publicar prenda para reventa C2C (>0 COP) o donación (0 COP), incluyendo atributos, `ConservationStatus` y precio | `USER`, `STORE`, `ADMIN` | `201`, `400`, `401`, `403`, `500` |
| **Second-Hand Publications** | `GET` | `/api/v1/second-hand/publications` | `SR-RF-36` | Explorar y buscar prendas (`?search=` / `?searchTerm=`, categoría, color, talla, localidad, precio, `AVAILABLE` por defecto) | Público | `200`, `400`, `500` |
| **Second-Hand Publications** | `GET` | `/api/v1/second-hand/publications/{id}` | `SR-RF-36` | Consultar detalle de publicación por ID | Público | `200`, `404`, `500` |
| **Second-Hand Publications** | `GET` | `/api/v1/second-hand/publications/users/{userId}` | `SR-P5-N04` | Consultar y gestionar listado de publicaciones propias del usuario | Propietario, `ADMIN` | `200`, `401`, `403`, `500` |
| **Second-Hand Publications** | `PUT` | `/api/v1/second-hand/publications/{id}` | `SR-RF-37` | Editar prenda (precio, fotos, talla, descripción `@NotBlank`) en estado `AVAILABLE` | Propietario, `ADMIN` | `200`, `400`, `401`, `403`, `404`, `409`, `500` |
| **Second-Hand Publications** | `PATCH` | `/api/v1/second-hand/publications/{id}/sale` | `SR-RF-38` | Marcar como vendida (`SOLD`), exigiendo `compradorId` y rechazando `interesadosIds` arbitrarios enviados desde cliente | Propietario, `ADMIN` | `200`, `400`, `401`, `403`, `404`, `409`, `500` |
| **Second-Hand Publications** | `DELETE` | `/api/v1/second-hand/publications/{id}` | `SR-RF-39` | Retirar publicación activa de la vitrina (bloquea `SOLD`, `IN_DONATION`, `DONATED`) | Propietario, `ADMIN` | `204`, `401`, `403`, `404`, `409`, `500` |
| **Second-Hand Publications** | `PATCH` | `/api/v1/second-hand/publications/{id}/donation` | `SR-RF-42` | Convertir publicación existente al canal benéfico (`IN_DONATION`, precio `0 COP`) | Propietario, `ADMIN` | `200`, `401`, `403`, `404`, `409`, `500` |
| **Charitable Donations** | `GET` | `/api/v1/donations/foundations` | `SR-RF-45` | Listar fundaciones aliadas activas desde `ms-tiendas` (filtro opcional `?category=`) | `USER`, `STORE`, `FOUNDATION`, `ADMIN` | `200`, `401`, `503` |
| **Charitable Donations** | `GET` | `/api/v1/donations/foundations/representative` | `SR-RF-45` | Obtener la fundación asociada al representante autenticado (`sub` ↔ `representanteId`) | `FOUNDATION`, `ADMIN` | `200`, `401`, `403`, `404`, `503` |
| **Charitable Donations** | `GET` | `/api/v1/donations/foundations/{foundationId}` | `SR-RF-45` | Consultar detalle institucional de una fundación activa en `ms-tiendas` | `USER`, `STORE`, `FOUNDATION`, `ADMIN` | `200`, `400`, `401`, `404`, `503` |
| **Charitable Donations** | `POST` | `/api/v1/donations/claims` | `SR-P5-N05` | Reclamar prenda disponible desde la Vitrina Abierta por una fundación con bloqueo optimista (`@Version`) | `FOUNDATION`, `ADMIN` | `201`, `400`, `401`, `403`, `404`, `409`, `503` |
| **Charitable Donations** | `POST` | `/api/v1/donations` | `SR-RF-45` | Crear solicitud de donación: resuelve `donorId` desde el token JWT cuando se omite y acepta objeto estructurado `deliveryProposal` (`deliveryMode`, `deliveryPoint`, `startTime`, `endTime`) o campos planos retrocompatibles | `USER`, `STORE`, `ADMIN` | `201`, `400`, `401`, `403`, `404`, `409`, `503` |
| **Charitable Donations** | `GET` | `/api/v1/donations/{id}` | `SR-RF-46` | Consultar estado y cita logística de una solicitud (protegido contra BOLA/IDOR) | Donante, Fundación, `ADMIN` | `200`, `401`, `403`, `404`, `500` |
| **Charitable Donations** | `GET` | `/api/v1/donations/donors/{donorId}` | `SR-RF-46` | Historial de solicitudes originadas por el donante | Donante propietario, `ADMIN` | `200`, `401`, `403`, `500` |
| **Charitable Donations** | `GET` | `/api/v1/donations/foundations/{foundationId}/requests` | `SR-RF-46` | Bandeja de solicitudes recibidas por la fundación (`?status=` opcional) | Fundación receptora / Representante, `ADMIN` | `200`, `401`, `403`, `500` |
| **Charitable Donations** | `PATCH` | `/api/v1/donations/{id}/decision` | `SR-RF-47` | Aceptar (`APPROVED`) o rechazar (`REJECTED`, revierte prenda a `AVAILABLE`) | Fundación receptora / Representante, `ADMIN` | `200`, `400`, `401`, `403`, `404`, `409`, `500` |
| **Charitable Donations** | `PATCH` | `/api/v1/donations/{id}/pickup` | `SR-RF-45` / `SR-P5-N05` | Agendar franja horaria futura (`America/Bogota`) y punto de entrega tras aprobación o reclamo | Donante, Fundación / Representante, `ADMIN` | `200`, `400`, `401`, `403`, `404`, `409`, `500` |
| **Charitable Donations** | `PATCH` | `/api/v1/donations/{id}/confirmation` | `SR-RF-49` | Confirmar recepción física (`DELIVERED`), marcar prenda `DONATED` y emitir comprobante `SR-DON-XXXX` | Fundación receptora / Representante, `ADMIN` | `200`, `400`, `401`, `403`, `404`, `409`, `500` |

---

[← Anterior: 3. Arquitectura y Diagramas](./03-arquitectura-diagramas.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 5. Eventos RabbitMQ →](./05-eventos-rabbitmq.md)
