[← Volver al README Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 2. Equipo y Responsables →](./02-equipo-responsables.md)

# 1. Nombre del Servicio y su Bounded Context

* **Identificador de Servicio:** `PT8080-Donations-Service` (`ms-segunda-vida-donaciones`)
* **Paquete Arquitectónico:** Paquete 5: Probador virtual + Donaciones + API Gateway
* **Puerto de Escucha Oficial:** `8520` (context-path: `/api/v1`)
* **Base de Datos Asignada:** `PostgreSQL 16` (`styleradar_donations_qa` en QA / `styleradar_donations_prod` en PROD), aisladas en Azure Flexible Server bajo el patrón *Database-per-Service*.
* **Almacenamiento de Fotografías:** `Azure Blob Storage` (Cuenta `srdonacionesjmunar`, contenedor público `donaciones-fotos` en `rg-styleradar`).
* **Broker de Mensajería:** `RabbitMQ 3.12` mediante el Topic Exchange compartido `styleradar.exchange` con *Publisher Confirms* y *Correlation ID* (AMQPS TLS puerto `5671`).
* **Bounded Context:** `Segunda Vida y Donaciones` (Economía Circular Textil C2C y Sostenibilidad Social en Bogotá D.C.)

---

## Responsabilidades del Dominio

Este microservicio encapsula exclusivamente la lógica de negocio de economía circular textil para **Bogotá D.C.** dividida en dos ejes:

1. **Compraventa C2C de prendas de segunda mano (`esDonacion = false`, precio > 0 COP):** Publicación con fotografías en Azure Blob Storage, filtros por las 20 localidades de Bogotá, integración con `ms-chat` y `ms-notificaciones`, y transición a `SOLD` mediante evento RabbitMQ con `compradorId` obligatorio.
2. **Vitrina Benéfica y Donaciones (`esDonacion = true`, precio 0 COP):** Publicación en Vitrina Abierta con reclamo directo por fundaciones aliadas, postulación directa del donante (`deliveryProposal` + `donorId` inferido del JWT), control de concurrencia optimista (`@Version`), prevención de auto-donación, coordinación de franjas horarias de entrega en Bogotá y confirmación física con evento `styleradar.donaciones.estado.cambiado`.

| Qué HACE este Microservicio (Alcance) | Qué NO HACE este Microservicio (Límites) |
| :--- | :--- |
| • Gestiona el catálogo circular C2C: publicación con fotos en Azure Blob Storage, categoría (9 oficiales), marca, color, estilo, talla (18 estandarizadas), estado de conservación (`ConservationStatus`), precio en COP, edición, venta y retiro voluntario (`SR-RF-33` a `39`). | • NO autentica usuarios ni emite identidades productivas (delegado a `ms-auth` en `:8081`; valida tokens JWT Bearer de forma stateless). |
| • Gestiona la Vitrina Abierta y solicitudes de donación: reclamo concurrente por fundaciones (`@Version`), selección de fundación activa, aprobación/rechazo, franjas de entrega en Bogotá y confirmación física (`SR-RF-42`, `45` a `49`, `SR-P5-N05`). | • NO procesa pasarelas de pago monetarias de tiendas comerciales (delegado a `ms-pagos` en `:8090`). |
| • Carga y sirve fotografías binarias de prendas directamente a **Azure Blob Storage** (`donaciones-fotos`) generando URLs públicas permanentes con fallback resiliente a almacenamiento local. | • NO envía correos ni notificaciones push directas al usuario final (delegado a `ms-notificaciones` en `:8300` vía eventos RabbitMQ). |
| • Aplica control de acceso basado en roles (`USER`, `STORE`, `FOUNDATION`, `ADMIN`) y validación estricta de propiedad (`ResourceOwnershipValidator`) contra ataques IDOR/BOLA (`SR-RNF-04`). | • NO gestiona salas de chat en tiempo real entre compradores y vendedores (delegado a `ms-chat` en `:8200`). |
| • Emite eventos AMQP trazables con `version = 1`, `eventoId` idempotente, fechas ISO-8601 sin zona horaria (`yyyy-MM-dd'T'HH:mm:ss`) y UUID de correlación hacia `styleradar.exchange`. | • NO accede directamente a bases de datos de otros microservicios bajo el principio *Database-per-Service*. |

---

[← Volver al README Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 2. Equipo y Responsables →](./02-equipo-responsables.md)
