[← Anterior: 8. Variables de Entorno](./08-variables-entorno.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 10. CI/CD y Pipelines →](./10-cicd-pipeline.md)

# 9. Pruebas y Cobertura (≥ 85%)

El proyecto exige una cobertura mínima de **líneas** del **85.00%** verificada automáticamente mediante **JaCoCo** durante la fase `verify` de Maven (`mvn -B clean verify` / `mvn -B clean verify "-Dapi.version=1.44"`).

## Resumen de Métricas de Pruebas y Cobertura

| Métrica | Valor Alcanzado | Entorno y Condición | Umbral Exigido | Respaldo / Enlace de Ejecución |
| :--- | :---: | :--- | :---: | :--- |
| **Pruebas Automatizadas (Entorno Local - Rama Actual)** | **309 pruebas aprobadas** *(de 310 totales en Surefire: 0 fallos, 0 errores, 1 clase omitida)* | **Local** (sin Docker: se omite la prueba condicional PostgreSQL `VitrinaAbiertaPostgresConcurrencyIntegrationTest`) | Suite completa sin fallos | Evidencia local [`docs/evidencias/`](../evidencias/) y comando `mvn -B clean verify` |
| **Pruebas Automatizadas (CI `develop` / `main` Baseline)** | **277** en `main` · **282** en `develop` (`952da52`) | **CI GitHub Actions** (`ubuntu-latest` con Docker Engine activo) | `≥ 85.00%` cobertura de líneas + 0 fallos | [Run CI QA `develop` (#45529263718)](https://github.com/StyleRadar1/PT8080-Donations-Service/actions/runs/45529263718) · [Run CD PROD `main` (#45525360439)](https://github.com/StyleRadar1/PT8080-Donations-Service/actions/runs/45525360439) |
| **Cobertura de Líneas (JaCoCo `LINE`)** | **97.36%** (`2544 / 2613`) | **Local** (`mvn -B clean verify`, 94 clases analizadas) | `≥ 85.00%` *(umbral obligatorio del gate)* | Reporte `target/site/jacoco/jacoco.xml` · [`cobertura-jacoco-sprint3.png`](../evidencias/cobertura-jacoco-sprint3.png) |
| **Cobertura de Instrucciones (JaCoCo `INSTRUCTION`)** | **97.33%** (`11143 / 11449`) | **Local** (`mvn -B clean verify`, 94 clases analizadas) | Referencia (`≥ 85.00%`) | Reporte `target/site/jacoco/jacoco.xml` · [Run CI `develop` (#45529263718)](https://github.com/StyleRadar1/PT8080-Donations-Service/actions/runs/45529263718) |
| **Cobertura de Ramas (JaCoCo `BRANCH`)** | **87.26%** (`774 / 887`) | **Local** (`mvn -B clean verify`, 94 clases analizadas) | Referencia (`≥ 85.00%`) | Reporte `target/site/jacoco/jacoco.xml` |
| **Cobertura de Métodos (JaCoCo `METHOD`)** | **95.30%** (`426 / 447`) | **Local** (`mvn -B clean verify`, 94 clases analizadas) | Referencia (`≥ 85.00%`) | Reporte `target/site/jacoco/jacoco.xml` |
| **Auditoría SonarQube** | **Quality Gate Passed (Rating A)** | **Análisis estático local / SonarQube** (0 Bugs, 0 Vulnerabilidades, 0 Code Smells críticos) | Quality Gate Aprobado | [`analisis-sonarqube.png`](../evidencias/analisis-sonarqube.png) |

---

## Pirámide de Pruebas Implementada

| Nivel de Prueba | Clases de Prueba Representativas | Alcance Verificado |
| :--- | :--- | :--- |
| **Dominio Puro (Unitarias)** | `SecondHandPublicationTest`, `DonationRequestTest`, `LocalidadBogotaTest`, `EnumsAndEventsTest` | Entidades, máquinas de estados, enums (`GarmentType`, `Size`, `Style`, `DeliveryMode`), invariantes de Bogotá y precio. |
| **Casos de Uso (Unitarias Mockito + E2E)** | `DonationCompleteFlowIntegrationTest`, `FoundationDeletedOutboxIntegrationTest`, `DonationDeliveryOutboxIntegrationTest`, `ManagePublicationUseCaseTest`, `CreateDonationRequestUseCaseTest`, `ClaimDonationUseCaseTest`, `GetPartnerFoundationsUseCaseTest`, `ConfirmDonationReceiptUseCaseTest`, `UploadGarmentPhotoUseCaseImplTest` | Los 10 casos de uso (`*UseCaseImpl`), los 7 escenarios del flujo benéfico completo, Outbox transaccional en cancelaciones y entregas (Notificaciones + Estadísticas independientes), concurrencia optimista y subida de imágenes. |
| **Controladores REST (`MockMvc`)** | `SecondHandPublicationControllerTest`, `DonationRequestControllerTest`, `PhotoUploadControllerTest`, `AuthControllerTest`, `HealthControllerTest` | Contratos HTTP (`deliveryProposal` + `donorId` resuelto por JWT, rechazo de `interesadosIds` desde cliente), códigos 200/201/204 y RFC 7807 (`ProblemDetail`) para 400/401/403/404/409/500/503. |
| **Persistencia, Storage, HTTP y Mensajería** | `SecondHandPublicationPersistenceAdapterTest`, `DonationRequestPersistenceAdapterTest`, `TiendasFoundationCatalogAdapterTest`, `AzureBlobStorageAdapterTest`, `DonationEventPublisherAdapterTest`, `DonationRequestCreatedIntegrationEventTest`, `RabbitMQTlsCertificateTest` | Mapeo JPA, cliente HTTP hacia `ms-tiendas`, Azure Blob Storage con fallback local, fechas ISO-8601 sin zona (`yyyy-MM-dd'T'HH:mm:ss`) y RabbitMQ TLS (`AMQPS 5671`). |
| **Seguridad e Integración RBAC** | `SecurityRbacIntegrationTest`, `AuthQaSecurityIsolationTest`, `JwtTokenProviderTest`, `ResourceOwnershipValidatorTest`, `DemoUserServiceTest` | JWT oficiales `ms-auth` (`sub` + `roles`), aislamiento en QA/Prod, roles (`USER`, `STORE`, `FOUNDATION`, `ADMIN`), mitigación BOLA/IDOR, CORS y `representanteId`. |

---

## Evidencias del Proyecto ([`docs/evidencias/`](../evidencias/))

📁 **Todas las evidencias de ejecución, pruebas, cobertura, calidad y despliegue se encuentran centralizadas en la carpeta [`docs/evidencias/`](../evidencias/)** (compilación Maven, reporte JaCoCo, auditoría SonarQube, Swagger UI, Health Check en Azure, contenedores Docker, RabbitMQ TLS y tablero Jira del squad).

---

[← Anterior: 8. Variables de Entorno](./08-variables-entorno.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 10. CI/CD y Pipelines →](./10-cicd-pipeline.md)
