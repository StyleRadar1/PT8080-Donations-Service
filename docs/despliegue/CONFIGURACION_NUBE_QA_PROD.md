# Guía Técnica de Despliegue en la Nube y CI/CD (QA & PROD)
## Microservicio: `ms-segunda-vida-donaciones` (StyleRadar · Puerto 8520)

**Asignatura:** Desarrollo y Operación de Software (DOSW) · 2026  
**Entregable:** Sprint 2 · Fase 5 (Págs. 8, 10, 18 y 19)  
**Alcance:** Pipeline Automatizado de Despliegue, Bases de Datos Aisladas en la Nube y Swagger Público  

---

## 1. Arquitectura de Despliegue y Separación Estricta de Entornos

Conforme a las directrices de la **Página 8** del proyecto, **los ambientes de QA y PRODUCCIÓN jamás comparten bases de datos ni recursos de infraestructura**. Se configuran dos instancias PostgreSQL completamente aisladas y dos entornos de ejecución desacoplados:

```mermaid
flowchart TD
    subgraph Git_Workflow["Flujo de Ramas Git"]
        DEV["Rama develop"]
        MASTER["Rama master / main"]
    end

    subgraph GitHub_Actions["Pipeline CI/CD (.github/workflows/ci.yml)"]
        BUILD["Build, Test & JaCoCo Coverage (≥ 85%)"]
        DOCKER["Empaquetado Docker OCI (Temurin 21)"]
        DEPLOY_QA["deploy-qa<br>(Despliegue Automático)"]
        DEPLOY_PROD["deploy-prod<br>(Aprobación Manual CMV)"]
    end

    subgraph QA_Environment["Entorno Staging / QA (Nube)"]
        APP_QA["Web Service QA<br>(ms-segunda-vida-donaciones)"]
        SWAGGER_QA["Swagger UI Público QA<br>(/api/v1/swagger-ui.html)"]
        DB_QA[("PostgreSQL QA en la Nube<br>(Supabase / Neon / Render QA)")]
        RABBIT_QA["RabbitMQ Cloud QA<br>(CloudAMQP TLS: 5671)"]
    end

    subgraph PROD_Environment["Entorno Producción / PROD (Nube)"]
        APP_PROD["Web Service PROD<br>(ms-segunda-vida-donaciones)"]
        SWAGGER_PROD["Swagger UI PROD<br>(/api/v1/swagger-ui.html)"]
        DB_PROD[("PostgreSQL PROD en la Nube<br>(Aislada e Independiente)")]
        RABBIT_PROD["RabbitMQ Cloud PROD<br>(CloudAMQP TLS: 5671)"]
    end

    DEV -->|Push / Merge| BUILD
    MASTER -->|Push / Merge con CMV| BUILD
    BUILD --> DOCKER
    DOCKER -->|Si es develop| DEPLOY_QA
    DOCKER -->|Si es master + CMV| DEPLOY_PROD

    DEPLOY_QA --> APP_QA
    APP_QA -.-> SWAGGER_QA
    APP_QA --> DB_QA
    APP_QA --> RABBIT_QA

    DEPLOY_PROD --> APP_PROD
    APP_PROD -.-> SWAGGER_PROD
    APP_PROD --> DB_PROD
    APP_PROD --> RABBIT_PROD

    classDef pass fill:#DCFCE7,stroke:#16A34A,stroke-width:2px,color:#0C0C12;
    classDef step fill:#12063A,stroke:#5600EF,stroke-width:2px,color:#FFFFFF;
    classDef db fill:#FEF3C7,stroke:#D97706,stroke-width:2px,color:#0C0C12;

    class BUILD,DOCKER,DEPLOY_QA,DEPLOY_PROD pass;
    class APP_QA,APP_PROD,SWAGGER_QA,SWAGGER_PROD step;
    class DB_QA,DB_PROD,RABBIT_QA,RABBIT_PROD db;
```

---

## 2. Matriz de Despliegue en la Nube (Pág. 8)

| Servicio | URL QA | URL PROD | BD QA (Aislada) | BD PROD (Aislada) |
| :--- | :--- | :--- | :--- | :--- |
| **`ms-segunda-vida-donaciones`** | `https://styleradar-donaciones-qa-jmunar.azurewebsites.net` | `https://styleradar-donaciones-prod-jmunar.azurewebsites.net` | `bluevelvet-db-qa-jmunar.postgres.database.azure.com` (Azure PostgreSQL QA) | `styleradar-db-prod-jmunar.postgres.database.azure.com` (Azure PostgreSQL PROD) |

* **Regla de Oro:** La base de datos de producción (`styleradar_donations_prod`) reside en un clúster físico o lógico independiente de QA (`styleradar_donations_qa`), con credenciales, usuarios y políticas de firewall distintas.

---

## 3. Swagger Público y Endpoints de Salud (Pág. 10 y 19)

### 3.1. Swagger UI de QA
El microservicio expone su documentación interactiva OpenAPI 3 en:
👉 **`https://styleradar-donaciones-qa-jmunar.azurewebsites.net/api/v1/swagger-ui.html`**  
*(OpenAPI JSON: `https://styleradar-donaciones-qa-jmunar.azurewebsites.net/api/v1/v3/api-docs`)*

### 3.2. Endpoints de Diagnóstico (`/health`)
Para la validación del estado operativo por parte de load balancers y herramientas de monitoreo:
* **Endpoint QA:** `GET https://styleradar-donaciones-qa-jmunar.azurewebsites.net/api/v1/health`
* **Endpoint PROD:** `GET https://styleradar-donaciones-prod-jmunar.azurewebsites.net/api/v1/health`

**Respuesta esperada (HTTP 200 OK):**
```json
{
  "status": "UP",
  "service": "ms-segunda-vida-donaciones",
  "port": 8520,
  "timestamp": "2026-11-09T20:00:00.000000Z"
}
```

---

## 4. Configuración de Variables de Entorno por Ambiente

El microservicio utiliza perfiles Spring Boot desacoplados (`application-qa.yml` y `application-prod.yml`), inyectando su configuración por variables de entorno del contenedor:

| Variable de Entorno | Valor en QA | Valor en PROD | Descripción |
| :--- | :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `qa` | `prod` | Activa el perfil correspondiente de Spring Boot. |
| `PORT` | `8520` (o asignado por cloud) | `8520` (o asignado por cloud) | Puerto de enlace del servidor embebido. |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<HOST_QA>:5432/styleradar_donations_qa?sslmode=require` | `jdbc:postgresql://<HOST_PROD>:5432/styleradar_donations_prod?sslmode=require` | Conexión JDBC segura con SSL obligatorio. |
| `DB_USER` | `styleradar_qa_user` | `styleradar_prod_user` | Usuario de base de datos exclusivo del entorno. |
| `DB_PASSWORD` | `<SECRET_QA>` | `<SECRET_PROD>` | Contraseña cifrada en la nube. |
| `SECURITY_JWT_SECRET` | Clave segura HMAC-SHA256 (≥ 256 bits) | Clave robusta de producción (≥ 256 bits) | Clave secreta para validación de tokens JWT. |
| `RABBITMQ_HOST` | `<HOST_CLOUDAMQP_QA>` | `<HOST_CLOUDAMQP_PROD>` | Broker RabbitMQ en la nube. |
| `RABBITMQ_PORT` | `5671` | `5671` | Puerto con cifrado TLS/SSL. |
| `RABBITMQ_SSL_ENABLED`| `true` | `true` | Cifrado en tránsito de eventos asíncronos. |
| `RABBITMQ_USERNAME` | `<USER_RABBITMQ_QA>` | `<USER_RABBITMQ_PROD>` | Usuario de conexión a RabbitMQ. |
| `RABBITMQ_PASSWORD` | `<PASS_RABBITMQ_QA>` | `<PASS_RABBITMQ_PROD>` | Contraseña del broker. |
| `RABBITMQ_VHOST` | `/<vhost_qa>` | `/<vhost_prod>` | Virtual Host asignado en CloudAMQP. |

---

## 5. Configuración de Secretos en GitHub Actions

Para que el pipeline `.github/workflows/ci.yml` ejecute el despliegue automático:

1. Ir a **Settings ➔ Secrets and variables ➔ Actions** en el repositorio GitHub.
2. Registrar los siguientes **Repository Secrets**:
   * `RENDER_DEPLOY_HOOK_QA`: URL del Webhook de despliegue automático de Render para el servicio QA.
   * `RENDER_DEPLOY_HOOK_PROD`: URL del Webhook de despliegue de Render para PROD.
   * `AZURE_WEBAPP_PUBLISH_PROFILE_QA`: (Opcional si se despliega en Azure App Service).
   * `AZURE_WEBAPP_PUBLISH_PROFILE_PROD`: (Opcional si se despliega en Azure App Service).
3. Registrar las siguientes **Repository Variables** (opcionales):
   * `QA_APP_URL`: `https://styleradar-donaciones-qa-jmunar.azurewebsites.net`
   * `PROD_APP_URL`: `https://styleradar-donaciones-prod-jmunar.azurewebsites.net`

---

## 6. Procedimiento de Aprovisionamiento Paso a Paso

### Paso 1: Aprovisionar Base de Datos PostgreSQL QA (Neon / Supabase / Render)
1. Crear un proyecto llamado `styleradar-donations-qa`.
2. Obtener la cadena de conexión JDBC con `?sslmode=require`.
3. Al arrancar el microservicio, Flyway ejecutará automáticamente los scripts `V1__init_schema.sql` y `V2__initial_seed_data.sql`, creando las tablas `publicaciones_segunda_mano`, `solicitudes_donacion`, `fotos_publicacion` e insertando la data semilla.

### Paso 2: Aprovisionar Base de Datos PostgreSQL PROD
1. Crear un proyecto independiente llamado `styleradar-donations-prod`.
2. Asignar credenciales dedicadas de producción.
3. Verificar que las tablas se migren limpiamente con Flyway en el primer arranque.

### Paso 3: Aprovisionar Broker CloudAMQP (RabbitMQ)
1. Crear una instancia en [CloudAMQP](https://www.cloudamqp.com) (plan gratuito Lemur).
2. Obtener el hostname AMQP, virtual host y credenciales.
3. El microservicio creará automáticamente el Topic Exchange `donaciones.exchange` y la cola durable `q.notificaciones.donacion-solicitud-creada`.
