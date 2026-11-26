[← Anterior: 4. Eventos RabbitMQ](./04-eventos-que-publica-y-consume.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 7. Ejecución Local y con Docker →](./06-como-ejecutar-local-y-docker.md)

# 5. Tecnologías y Dependencias

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
| **Testing Automatizado** | JUnit 5 + Mockito + H2 + Testcontainers | `5.10.3` | **339 pruebas aprobadas** *(0 fallos, 0 errores, 0 omitidas con Docker/Testcontainers PostgreSQL activo)*; último `develop` aprobado y desplegado en QA ([Run #45728635298](https://github.com/StyleRadar1/PT8080-Donations-Service/actions/runs/45728635298)). Ver [detalle y enlaces](./08-pruebas-y-cobertura.md). |
| **Métricas de Cobertura** | JaCoCo Maven Plugin | `0.8.12` | Verificación estricta de cobertura mínima (`≥ 85%` de líneas en `verify`), alcanzando: **97.50% líneas (`2770/2841`)**, **97.52% instrucciones (`12167/12477`)**, **88.24% ramas (`915/1037`)** y **95.83% métodos (`460/480`)**. |

---

[← Anterior: 4. Eventos RabbitMQ](./04-eventos-que-publica-y-consume.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 7. Ejecución Local y con Docker →](./06-como-ejecutar-local-y-docker.md)
