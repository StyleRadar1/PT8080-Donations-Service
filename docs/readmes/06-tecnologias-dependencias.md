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
| **Control de Esquema** | Flyway Core | `10.10.0` | Migraciones reproducibles (`V1__init_schema.sql` .. `V5__migrate_garment_type_and_size_enums.sql`). |
| **Broker de Mensajería** | RabbitMQ (Spring AMQP) | `3.12` | Topic Exchanges durables sobre AMQPS TLS `5671` con *Publisher Confirms* y certificados en `certs/`. |
| **Mapeo y Boilerplate** | MapStruct + Lombok | `1.5.5` / `1.18.38` | Reducción de código repetitivo compatible con JDK 21. |
| **Documentación API** | SpringDoc OpenAPI | `2.5.0` | Especificación OpenAPI 3.0 en inglés técnico, esquemas RFC 7807 y ejemplos interactivos. |
| **Testing Automatizado** | JUnit 5 + Mockito + H2 + Testcontainers | `5.10.3` | **276 pruebas pasando en local** *(277 totales: 0 fallos, 0 errores, 1 prueba PostgreSQL condicional para CI omitida en local)*. |
| **Métricas de Cobertura** | JaCoCo Maven Plugin | `0.8.12` | Verificación estricta de cobertura mínima (≥ 85%) alcanzando **98.20% líneas / 98.07% instrucciones / 88.80% ramas / 96.30% métodos**. |

---

[← Anterior: 5. Eventos RabbitMQ](./05-eventos-rabbitmq.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 7. Ejecución Local y con Docker →](./07-ejecucion-local-docker.md)
