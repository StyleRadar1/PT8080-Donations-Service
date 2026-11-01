# StyleRadar · ms-segunda-vida-donaciones

Microservicio de **Donaciones y Economía Circular / Segunda Vida** perteneciente al Paquete 5 de la plataforma **StyleRadar** para la asignatura DOSW.

## 🏛️ Arquitectura Hexagonal (Puertos y Adaptadores)

Este microservicio sigue de forma estricta los principios de **Arquitectura Hexagonal (Puertos y Adaptadores)** de Alistair Cockburn:

```text
.
├── .dockerignore
├── .gitignore
├── Dockerfile
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/edu/dosw/styleradar/donaciones/
    │   │   ├── DonationsServiceApplication.java
    │   │   ├── domain/                         # 1. NÚCLEO PURO (Sin Frameworks ni JPA/Spring)
    │   │   │   ├── model/                      # Entidades de Dominio y Value Objects
    │   │   │   ├── event/                      # Eventos puros de dominio
    │   │   │   ├── exception/                  # Excepciones de negocio
    │   │   │   └── port/
    │   │   │       ├── in/                     # Puertos de Entrada (Use Cases)
    │   │   │       └── out/                    # Puertos de Salida (SPI: Repositorios, Eventos)
    │   │   ├── application/                    # 2. ORQUESTACIÓN
    │   │   │   ├── usecase/                    # Implementación de Casos de Uso (*UseCaseImpl)
    │   │   │   ├── service/                    # Interfaces de Servicios de Aplicación
    │   │   │   │   └── impl/                   # Implementación de Servicios (*ServiceImpl)
    │   │   │   ├── dto/                        # Objetos de Transferencia de Datos
    │   │   │   │   ├── request/                # DTOs de Entrada (Payloads inmutables)
    │   │   │   │   ├── response/               # DTOs de Salida (Respuestas al cliente)
    │   │   │   │   └── event/                  # DTOs de Mensajería / Broker
    │   │   │   └── mapper/                     # Mappers MapStruct segregados
    │   │   │       ├── in/                     # Request -> Modelo
    │   │   │       └── out/                    # Modelo -> Response / Event
    │   │   └── infrastructure/                 # 3. ADAPTADORES & SPRING
    │   │       ├── adapter/
    │   │       │   ├── in/                     # Adaptadores Driving (Entrada)
    │   │       │   │   ├── rest/               # Controladores REST (@RestController)
    │   │       │   │   │   └── doc/            # Especificación y documentación OpenAPI
    │   │       │   │   ├── amqp/               # Listeners RabbitMQ (@RabbitListener)
    │   │       │   │   └── websocket/          # Handlers WebSocket / STOMP
    │   │       │   └── out/                    # Adaptadores Driven (Salida)
    │   │       │       ├── persistence/        # Persistencia PostgreSQL (Spring Data JPA)
    │   │       │       │   ├── entity/         # Entidades JPA (@Entity, @Table)
    │   │       │       │   └── repository/     # Repositorios Spring Data
    │   │       │       ├── messaging/          # Publicadores AMQP / RabbitMQ
    │   │       │       ├── security/           # Seguridad y Proveedor de Tokens
    │   │       │       └── external/           # Clientes HTTP y APIs Externas
    │   │       ├── config/                     # Configuración IoC y Beans de Spring
    │   │       └── exception/                  # GlobalExceptionHandler (RFC 7807 Problem Details)
    │   └── resources/
    │       ├── application.yml                 # Puerto 8093, context-path /api/v1
    │       ├── application-dev.yml             # Perfil local de desarrollo
    │       └── db/
    │           └── migration/
    │               └── V1__init_schema.sql     # Script inicial de migración Flyway
    └── test/                                   # ESPEJO EXACTO DE MAIN
        ├── java/edu/dosw/styleradar/donaciones/
        │   ├── domain/                         # Pruebas Unitarias de Dominio (JUnit 5, 0% Spring)
        │   ├── application/                    # Pruebas Unitarias de Casos de Uso (Mockito)
        │   └── infrastructure/                 # Pruebas de Integración (Testcontainers)
        └── resources/
            └── application-test.yml            # Configuración para ejecución de pruebas
```

---

## ⚙️ Especificaciones Técnicas

- **Lenguaje:** Java 21 LTS (Eclipse Temurin)
- **Framework:** Spring Boot 3.3.4
- **Puerto Asignado:** `8093`
- **Context Path:** `/api/v1`
- **Documentación OpenAPI:** `/api/v1/swagger-ui.html` | `/api/v1/v3/api-docs`
- **Base de Datos:** PostgreSQL 15+ con migraciones Flyway
- **Broker de Mensajería:** RabbitMQ (AMQP)
- **Cobertura de Código:** Meta mínima del 85% de cobertura de líneas exigida por JaCoCo

---

## 🔐 Variables de Entorno

| Variable | Descripción | Valor por Defecto |
| :--- | :--- | :--- |
| `SERVER_PORT` | Puerto de escucha HTTP | `8093` |
| `DB_HOST` | Host de PostgreSQL | `localhost` |
| `DB_PORT` | Puerto de PostgreSQL | `5432` |
| `DB_NAME` | Nombre de la base de datos | `styleradar_donations` |
| `DB_USER` | Usuario de PostgreSQL | `postgres` |
| `DB_PASSWORD` | Contraseña de PostgreSQL | `postgres` |
| `RABBITMQ_HOST` | Host de RabbitMQ | `localhost` |
| `RABBITMQ_PORT` | Puerto de RabbitMQ | `5672` |
| `RABBITMQ_USER` | Usuario de RabbitMQ | `guest` |
| `RABBITMQ_PASSWORD` | Contraseña de RabbitMQ | `guest` |
| `RABBITMQ_VHOST` | Virtual Host de RabbitMQ | `/` |

---

## 🚀 Comandos Rápidos

### Compilación y Verificación
```bash
# Compilar código fuente
mvn clean compile

# Ejecutar pruebas unitarias y de integración
mvn clean test

# Verificar cobertura de código con JaCoCo (85% mínimo)
mvn verify
```

### Construcción y Ejecución Docker
```bash
# Construir imagen multi-stage de producción
docker build -t ms-segunda-vida-donaciones:latest .

# Ejecutar contenedor
docker run -p 8093:8093 --name ms-donaciones ms-segunda-vida-donaciones:latest
```
