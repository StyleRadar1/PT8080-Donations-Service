[← Anterior: 7. Ejecución Local y con Docker](./07-ejecucion-local-docker.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 9. Pruebas y Cobertura →](./09-pruebas-cobertura.md)

# 8. Variables de Entorno

Todas las configuraciones se gestionan de forma desacoplada conforme a los principios de **Twelve-Factor App**:

| Variable | Descripción | Valor por Defecto (`dev`/`local`) | Requerido en Prod |
| :--- | :--- | :--- | :---: |
| `SERVER_PORT` / `PORT` | Puerto HTTP de escucha | `8520` | Sí |
| `SPRING_PROFILES_ACTIVE` | Perfil activo (`local`, `dev`, `qa`, `prod`, `test`) | `local` | Sí |
| `DB_HOST` | Hostname de PostgreSQL | `localhost` | Sí |
| `DB_PORT` | Puerto de PostgreSQL | `5434` (`5432` en Azure) | Sí |
| `DB_NAME` | Nombre de la base de datos | `styleradar_donations` | Sí |
| `DB_USER` | Usuario de base de datos | `styleradar_user` | Sí |
| `DB_PASSWORD` | Contraseña de base de datos | `<configured_in_env>` | Sí |
| `DB_SSLMODE` | Modo SSL JDBC para Azure | vacío (`require` en Cloud) | Sí |
| `AZURE_STORAGE_CONNECTION_STRING` | Connection String de Azure Blob Storage | *(vacío activa fallback local)* | Opcional |
| `AZURE_STORAGE_CONTAINER_NAME` | Contenedor público de blobs | `donaciones-fotos` | Sí |
| `AZURE_STORAGE_BLOB_ENDPOINT` | URL base del storage account | `https://srdonacionesjmunar.blob.core.windows.net` | Sí |
| `RABBITMQ_HOST` | Hostname del broker RabbitMQ | `localhost` (`styleradar-rabbitmq-qa.northcentralus.cloudapp.azure.com` en QA) | Sí |
| `RABBITMQ_PORT` | Puerto AMQP (`5672` local / `5671` AMQPS TLS nube) | `5672` (`5671` en QA/PROD) | Sí |
| `RABBITMQ_USER` / `RABBITMQ_USERNAME` | Usuario de conexión RabbitMQ | `guest` (`ms_donaciones` en QA) | Sí |
| `RABBITMQ_PASSWORD` | Contraseña de conexión RabbitMQ | `<configured_in_env>` | Sí |
| `RABBITMQ_VHOST` | Virtual host de RabbitMQ | `/` (`prod` en PROD) | Sí |
| `RABBITMQ_SSL_ENABLED` | Habilita AMQPS TLS en RabbitMQ | `false` (`true` en QA/PROD) | Sí |
| `RABBITMQ_TRUSTSTORE_PATH` | Ruta del truststore PKCS12/JKS para AMQPS | `classpath:certs/rabbitmq-truststore.jks` | Sí |
| `RABBITMQ_TRUSTSTORE_PASSWORD` | Contraseña del truststore RabbitMQ | `changeit` | Sí |
| `RABBITMQ_TRUSTSTORE_TYPE` | Tipo de keystore del truststore | `PKCS12` | Opcional |
| `RABBITMQ_SSL_VERIFY_HOSTNAME` | Verificación estricta de hostname en certificado AMQPS *(en QA puede ajustarse a `false` temporalmente solo si el host interno carece de SAN)* | `true` | Sí (`true` en `prod`) |
| `RABBITMQ_DECLARE_TOPOLOGY` | Declara colas/exchanges al arrancar (`false` para usuario publicador; requiere `styleradar.exchange` y bindings creados en el vhost destino) | `true` (`false` en QA/PROD) | Opcional |
| `RABBITMQ_EXCHANGE` | Exchange compartido de eventos | `styleradar.exchange` | Sí |
| `RABBITMQ_CHAT_EXCHANGE` | Exchange para eventos hacia chat | `styleradar.exchange` | Sí |
| `RABBITMQ_ROUTING_KEY_DONACION_CREATED` | Routing key de nueva solicitud hacia `ms-notificaciones` | `styleradar.donaciones.solicitud.creada` | Sí |
| `RABBITMQ_ROUTING_KEY_DONACION` | Routing key de cambio de estado hacia `ms-notificaciones` | `styleradar.donaciones.estado.cambiado` | Sí |
| `RABBITMQ_ROUTING_KEY_SEGUNDA_VIDA` | Routing key de venta hacia `ms-chat` | `segunda-mano.prenda.vendida` | Sí |
| `RABBITMQ_ROUTING_KEY_NOTIFICACIONES_SEGUNDA_VIDA` | Routing key de venta hacia `ms-notificaciones` | `styleradar.segundavida.prenda.vendida` | Sí |
| `JWT_SECRET_BASE64` | Clave Base64 HMAC-SHA256 compartida con `ms-auth` (≥ 256 bits) | Clave local dev/test | Sí |
| `TIENDAS_SERVICE_URL` | URL base de `ms-tiendas` para fundaciones (`SR-RF-45`) | `http://localhost:8140` (`qa`/`prod` Azure) | Sí |
| `TIENDAS_FOUNDATIONS_PATH` | Ruta del listado público de fundaciones en `ms-tiendas` | `/api/v1/foundations` | Opcional |
| `TIENDAS_FOUNDATION_BY_ID_PATH` | Ruta interna de detalle por ID (si está vacía, resuelve desde el listado general) | `/api/v1/internal/foundations/{id}` | Opcional |
| `TIENDAS_FOUNDATION_BY_REPRESENTATIVE_PATH` | Ruta interna de consulta por `representativeId` (si está vacía, resuelve desde el listado general) | `/api/v1/internal/foundations?representativeId={representativeId}` | Opcional |
| `TIENDAS_INTERNAL_USERNAME` | Usuario HTTP Basic para endpoints internos de `ms-tiendas` | `ms-donaciones` | Sí (QA/PROD) |
| `TIENDAS_INTERNAL_PASSWORD` | Contraseña HTTP Basic para endpoints internos de `ms-tiendas` | Vacío *(secreto en Azure)* | Sí (QA/PROD) |
| `TIENDAS_VALIDATION_ENABLED` | Activa/desactiva validación remota de fundaciones en `ms-tiendas` | `true` (`false` en test) | Opcional |
| `TIENDAS_SERVICE_TOKEN` | Token Bearer de servicio/admin para consultar endpoints protegidos de `ms-tiendas` | Vacío *(configurable)* | Opcional |
| `TIENDAS_FORWARD_BEARER_TOKEN` | Reenvía el token `Authorization: Bearer` del usuario entrante | `false` (`false` en `qa`/`prod`) | Opcional |
| `TIENDAS_SERVICE_ROLES_HEADER` | Nombre de la cabecera de rol de servicio esperada por `ms-tiendas` | `X-User-Roles` | Opcional |
| `TIENDAS_SERVICE_ROLES_VALUE` | Valor de la cabecera de rol para consultar el catálogo en `ms-tiendas` | `ROLE_ADMIN_STYLERADAR` | Opcional |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos para CORS del Frontend | `*` | Opcional |

---

[← Anterior: 7. Ejecución Local y con Docker](./07-ejecucion-local-docker.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 9. Pruebas y Cobertura →](./09-pruebas-cobertura.md)
