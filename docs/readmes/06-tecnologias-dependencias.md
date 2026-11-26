[← Anterior: 5. Eventos RabbitMQ](./05-eventos-rabbitmq.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 7. Ejecución Local y con Docker →](./07-ejecucion-local-docker.md)

# 6. Tecnologías y Dependencias

El stack tecnológico de `PT8080-Donations-Service` cumple con el estándar unificado de DOSW 2026:

| Capa / Componente | Tecnología | Versión | Justificación Técnica |
| :--- | :--- | :--- | :--- |
| **Lenguaje Core** | Java LTS (Eclipse Temurin) | `21` | Virtual Threads, Records inmutables y Pattern Matching. |
| **Framework Base** | Spring Boot | `3.3.4` | Soporte nativo para WebMVC, Data JPA, Security, AMQP y Actuator. |
| **Cloud Storage** | Azure Blob Storage SDK | `12.28.0` | `com.azure:azure-storage-blob` para almacenamiento de fotos en nube con fallback local. |
| **Seguridad y Tokens** | Spring Security + JJWT | `6.3.x` / `0.13.0` | Validación stateless de JWT con firma HMAC-SHA256 (`HS256`) y claim `exp`. |
| **Base de Datos** | PostgreSQL | `16` | Integridad ACID, bloqueo optimista (`@Version`) y aislamiento *Database-per-Service*. |
| **Control de Esquema** | Flyway Core | `10.10.0` | Migraciones reproducibles (`V1__init_schema.sql` .. `V8__extend_donation_outbox_for_delivery_events.sql`). |
| **Broker de Mensajería** | RabbitMQ (Spring AMQP) | `3.12` | Topic Exchanges durables sobre AMQPS TLS `5671` con *Publisher Confirms*, Outbox transaccional y certificados en `certs/`. |
| **Mapeo y Boilerplate** | MapStruct + Lombok | `1.5.5` / `1.18.38` | Reducción de código repetitivo compatible con JDK 21. |
| **Documentación API** | SpringDoc OpenAPI | `2.5.0` | Especificación OpenAPI 3.0 en inglés técnico, esquemas RFC 7807 y ejemplos interactivos. |
| **Testing Automatizado** | JUnit 5 + Mockito + H2 + Testcontainers | `5.10.3` | **330 pruebas aprobadas en entorno local** *(de 331 totales en Surefire: 0 fallos, 0 errores, 1 clase condicional PostgreSQL omitida sin Docker)*; último PR #22 en CI con Docker: aprobado ([Run #45728107219](https://github.com/StyleRadar1/PT8080-Donations-Service/actions/runs/45728107219)). Ver [detalle y enlaces](./09-pruebas-cobertura.md). |
| **Métricas de Cobertura** | JaCoCo Maven Plugin | `0.8.12` | Verificación estricta de cobertura mínima (`≥ 85%` de líneas en `verify`), alcanzando en local: **96.62% líneas (`2744/2840`)**, **96.49% instrucciones (`12032/12470`)**, **84.02% ramas (`873/1039`)** y **95.62% métodos (`459/480`)**. |

---

[← Anterior: 5. Eventos RabbitMQ](./05-eventos-rabbitmq.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 7. Ejecución Local y con Docker →](./07-ejecucion-local-docker.md)
