[← Anterior: 4. Endpoints y Swagger](./04-endpoints-swagger.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 6. Tecnologías y Dependencias →](./06-tecnologias-dependencias.md)

# 5. Eventos que Publica y Consume (RabbitMQ)

La integración asíncrona se realiza a través de **RabbitMQ 3.12** mediante conexión segura **AMQPS con TLS (puerto `5671`)** y Topic Exchange compartido en StyleRadar:

* **Exchange Principal y Chat:** `styleradar.exchange` (`topic`, durable)
* **Dead Letter Exchange (DLX):** `styleradar.dlx` (`direct`, durable; cola local `styleradar.donaciones.dlq` / cola notificaciones `styleradar.notificaciones.dlq`)
* **Formato de Fechas en Eventos (`ms-notificaciones`):** ISO-8601 sin zona horaria (`yyyy-MM-dd'T'HH:mm:ss`, zona `America/Bogota`, generado con `IntegrationDateFormatter`), ej. `2026-11-18T10:30:00`.
* **Declaración de Topología por Entorno:** En `qa` y `prod`, `app.rabbitmq.topology.enabled=false` permite operar con usuario de menor privilegio (`ms_donaciones`, solo publicación sobre `styleradar.exchange`) sin declarar colas al arranque. Esto **requiere que la topología base exista previamente en cada vhost del broker** (`styleradar.exchange` tipo `topic`, `styleradar.dlx` tipo `direct` y las colas/bindings consumidores de `ms-notificaciones` y `ms-chat`). En **QA (`/`)** se verificó conexión AMQPS con `verify-hostname=true` y existencia activa de `styleradar.exchange`, `styleradar.dlx` y `styleradar.notificaciones.{solicitud-donacion,donacion-cambiada,prenda-vendida}.q`; antes de activar **PROD (`prod`)** debe aprovisionarse la misma topología e usuario en el vhost `prod`.
* **Seguridad TLS AMQPS:** Tanto `prod` como `qa` habilitan `RABBITMQ_SSL_VERIFY_HOSTNAME=true` por defecto contra `rabbitmq-truststore.jks` (`StyleRadar-Root-CA`, cuyo certificado QA incluye SAN para `styleradar-rabbitmq-qa.northcentralus.cloudapp.azure.com` y `20.88.7.76`). Solo en **QA** puede usarse temporalmente `RABBITMQ_SSL_VERIFY_HOSTNAME=false` si se apunta a un alias interno sin SAN.
* **Garantías de Entrega:** *Publisher Confirms* (`correlated`), *Publisher Returns* y trazabilidad con `eventoId` idempotente, `version = 1` y `correlationId` UUID.

---

## Matriz de Eventos AMQP Integrados

| Tipo | Exchange | Routing Key | Emisor ➔ Receptor | Payload Homologado y Propósito de Integración |
| :---: | :--- | :--- | :--- | :--- |
| **PUBLICA** | `styleradar.exchange` | `segunda-mano.prenda.vendida` *(chat)* / `styleradar.segundavida.prenda.vendida` *(notif.)* | `ms-segunda-vida-donaciones` ➔ `ms-chat` & `ms-notificaciones` | `{ "eventoId": "...", "publicacionId": 77, "nombrePrenda": "Chaqueta bomber", "vendedorId": 101, "compradorId": 102, "interesadosIds": [], "fechaEvento": "2026-11-18T12:00:00", "version": 1 }`<br>Notifica venta C2C en Segunda Vida. El controlador rechaza `interesadosIds` enviados desde el cliente (`HTTP 400`), quedando la lista vacía hasta integración futura desde `ms-chat`. |
| **PUBLICA** | `styleradar.exchange` | `styleradar.donaciones.solicitud.creada` | `ms-segunda-vida-donaciones` ➔ `ms-notificaciones` | `{ "eventoId": "...", "donacionId": 40, "solicitudId": 40, "fundacionUsuarioId": 620, "representanteId": 620, "resumen": "Chaqueta Denim Vintage", "fechaEvento": "2026-11-18T10:30:00" }`<br>Dispara notificación `SOLICITUD_DONACION` al representante (`fundacionUsuarioId`) de la fundación con enlace `/donaciones/{donacionId}`. |
| **PUBLICA** | `styleradar.exchange` | `styleradar.donaciones.estado.cambiado` | `ms-segunda-vida-donaciones` ➔ `ms-notificaciones` | `{ "eventoId": "...", "donacionId": 40, "donanteId": 501, "nuevoEstado": "APROBADA | RECHAZADA | CITA_PROGRAMADA | ENTREGADA", "motivo": "...", "fechaCambio": "2026-11-18T11:00:00", "fechaEvento": "2026-11-18T11:00:00" }`<br>Dispara notificación `CAMBIO_DONACION` al donante con enlace `/donaciones/{donacionId}`. El estado `CITA_PROGRAMADA` contiene `CITA` según contrato de `ms-notificaciones`. |
| **CONSUME** | `styleradar.exchange` | `styleradar.donaciones.solicitud.creada` | `styleradar.exchange` ➔ `NotificationConsumerSimulator` | Consumidor de prueba E2E aislable mediante `RABBITMQ_SIMULATOR_ENABLED=true` (deshabilitado en QA/PROD) para diagnóstico local sin servicios externos. |

---

[← Anterior: 4. Endpoints y Swagger](./04-endpoints-swagger.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 6. Tecnologías y Dependencias →](./06-tecnologias-dependencias.md)
