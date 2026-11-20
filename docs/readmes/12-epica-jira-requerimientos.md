[← Anterior: 11. Ramas y Cómo Contribuir](./11-ramas-contribuir.md) &nbsp;|&nbsp; [Índice Principal](../../README.md)

# 12. Enlace a la Épica en Jira y Requerimientos

* **Repositorio Oficial en GitHub:** [https://github.com/StyleRadar1/PT8080-Donations-Service](https://github.com/StyleRadar1/PT8080-Donations-Service)
* **Épica Oficial del Microservicio (Jira):** [https://mail-team-nyjtgqcj.atlassian.net/browse/PT8080-2](https://mail-team-nyjtgqcj.atlassian.net/browse/PT8080-2)
* **Tablero Scrum del Squad Portal 8080 (Jira):** [https://mail-team-nyjtgqcj.atlassian.net/jira/software/projects/PT8080/boards/74](https://mail-team-nyjtgqcj.atlassian.net/jira/software/projects/PT8080/boards/74)
* **Contenedor OCI Oficial (Docker Hub):** [https://hub.docker.com/r/judamuch09/pt8080-donations-service-ms-segunda-vida-donaciones](https://hub.docker.com/r/judamuch09/pt8080-donations-service-ms-segunda-vida-donaciones)

---

## Matriz de los 12 Requerimientos Funcionales Reestructurados

> **Consolidación oficial Paquete 5:** `SR-RF-33` absorbe a `SR-RF-34` (estado de conservación) y `SR-RF-35` (precio), y `SR-RF-45` absorbe a `SR-RF-44` (selección de fundación) y `SR-RF-48` (propuesta de entrega). Los códigos `SR-P5-N04` y `SR-P5-N05` corresponden a **códigos provisionales de Paquete 5** pendientes de asignación de código estándar `SR-RF-NN` en la matriz maestra del proyecto.

| Código RF | Nombre del Requerimiento | Implementación en `ms-segunda-vida-donaciones` | Estado |
| :---: | :--- | :--- | :---: |
| `SR-RF-33` | Publicar prenda para venta de segunda mano o donación | `POST /api/v1/second-hand/publications` y `POST /api/v1/second-hand/photos`: categorías (9), tallas (18), color, localidad Bogotá, `ConservationStatus`, precio (>0 venta / 0 donación), descripción `@NotBlank` y fotos en Azure Blob Storage. *(Absorbe `SR-RF-34` y `SR-RF-35`)*. | ✅ Completado |
| `SR-RF-36` | Explorar y buscar prendas del catálogo general de Segunda Vida | `GET /api/v1/second-hand/publications` y `/{id}` (`GetPublicationsUseCaseImpl` + `PublicationSpecifications`): estado `AVAILABLE` por defecto, filtros por `?search=` / `?searchTerm=`, categoría, color, talla, conservación, precio, marca, estilo, `esDonacion` y localidad. | ✅ Completado |
| `SR-P5-N04` *(provisional)* | Consultar y gestionar publicaciones propias | `GET /api/v1/second-hand/publications/users/{userId}` con validación JWT y control de propiedad (`ResourceOwnershipValidator`). *(Pendiente de numeración `SR-RF-NN` en matriz maestra)*. | ✅ Completado |
| `SR-RF-37` | Editar publicación de segunda mano | `PUT /api/v1/second-hand/publications/{id}` (`ManagePublicationUseCaseImpl.edit`) restringido a estado editable (bloquea `SOLD`, `RETIRED`, `IN_DONATION`, `DONATED`) y descripción `@NotBlank`. | ✅ Completado |
| `SR-RF-38` | Marcar prenda de segunda mano como vendida | `PATCH /api/v1/second-hand/publications/{id}/sale` (`ManagePublicationUseCaseImpl.markAsSold`), con `compradorId` obligatorio, rechazo HTTP `400` si el cliente envía `interesadosIds` arbitrarios y eventos RabbitMQ homologados hacia `ms-chat` y `ms-notificaciones`. | ✅ Completado |
| `SR-RF-39` | Retirar publicación de segunda mano | `DELETE /api/v1/second-hand/publications/{id}` (`ManagePublicationUseCaseImpl.retire`), bloqueando `SOLD`, `IN_DONATION` y `DONATED`. | ✅ Completado |
| `SR-RF-42` | Convertir una publicación existente de venta en donación | `PATCH /api/v1/second-hand/publications/{id}/donation` (`ManagePublicationUseCaseImpl.markForDonation`) fijando `esDonacion=true`, `price=0` y `status=IN_DONATION`. | ✅ Completado |
| `SR-RF-45` | Crear solicitud de donación con fundación y propuesta de entrega | `POST /api/v1/donations` (`CreateDonationRequestUseCaseImpl`) + `GET /api/v1/donations/foundations*` y `PATCH /api/v1/donations/{id}/pickup`: obtiene `donorId` exclusivamente desde el token JWT (ignorando cualquier valor enviado en el payload), acepta objeto `deliveryProposal` (`deliveryMode`, `deliveryPoint`, `startTime`, `endTime`) o campos planos, valida fundación activa en `ms-tiendas` y publica evento con `representanteId` (`fundacionUsuarioId`). *(Absorbe `SR-RF-44` y `SR-RF-48`)*. | ✅ Completado |
| `SR-P5-N05` *(provisional)* | Reclamar prenda disponible desde la Vitrina Abierta | `POST /api/v1/donations/claims` (`ClaimDonationUseCaseImpl`) con bloqueo optimista (`@Version`, `409 Conflict` ante concurrencia), prevención de auto-donación y validación `ms-tiendas`. *(Pendiente de numeración `SR-RF-NN` en matriz maestra)*. | ✅ Completado |
| `SR-RF-46` | Consultar solicitudes de donación | `GET /api/v1/donations/{id}` (con validación BOLA/IDOR), `/donors/{donorId}`, `/foundations/{foundationId}/requests` y `/foundations/representative`. | ✅ Completado |
| `SR-RF-47` | Aceptar o rechazar solicitud de donación | `PATCH /api/v1/donations/{id}/decision` (`DecideDonationRequestUseCaseImpl`) con motivo obligatorio al rechazar y retorno automático de la prenda a `AVAILABLE`. | ✅ Completado |
| `SR-RF-49` | Confirmar recepción de donación | `PATCH /api/v1/donations/{id}/confirmation` (`ConfirmDonationReceiptUseCaseImpl`) transicionando solicitud a `DELIVERED`, prenda a `DONATED` y emitiendo comprobante `SR-DON-XXXX`. | ✅ Completado |

> **Nota de flujo logístico (`SR-RF-45` → `SR-RF-48`):** El servicio adopta el flujo secuencial realista de donaciones (`POST /api/v1/donations` en estado `PENDING` → `PATCH /api/v1/donations/{id}/decision` para aceptar/rechazar → `PATCH /api/v1/donations/{id}/pickup` para agendar franja futura y punto de entrega con evento `CITA_PROGRAMADA` → `PATCH /api/v1/donations/{id}/confirmation` para generar el comprobante `SR-DON-XXXX`).

---

[← Anterior: 11. Ramas y Cómo Contribuir](./11-ramas-contribuir.md) &nbsp;|&nbsp; [Índice Principal](../../README.md)
