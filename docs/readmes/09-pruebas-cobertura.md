[← Anterior: 8. Variables de Entorno](./08-variables-entorno.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 10. CI/CD y Pipelines →](./10-cicd-pipeline.md)

# 9. Pruebas y Cobertura (≥ 85%)

El proyecto exige una cobertura mínima de **líneas** del **85.00%** verificada automáticamente mediante **JaCoCo** durante la fase `verify` de Maven (`mvn -B clean verify` / `mvn -B clean verify "-Dapi.version=1.44"`).

## Resumen de Métricas de Pruebas y Cobertura

| Métrica | Valor Alcanzado | Entorno y Condición | Umbral Exigido | Respaldo / Enlace de Ejecución |
| :--- | :---: | :--- | :---: | :--- |
| **Pruebas Automatizadas (Suite Completa con Docker/Testcontainers)** | **339 pruebas aprobadas** *(0 fallos, 0 errores, 0 omitidas en Surefire)* | **Local con Docker** (incluye `VitrinaAbiertaPostgresConcurrencyIntegrationTest` en PostgreSQL 16 real) | Suite completa sin fallos | Evidencia local [`docs/evidencias/`](../evidencias/) y comando `mvn -B clean verify` |
| **Pruebas Automatizadas (CI `develop` / PR #22 / `main`)** | **`develop` (`4a2cbec`) aprobado y desplegado en QA** · PR #22 (`5f896e9`) aprobado · `main` (**277**) | **CI GitHub Actions** (`ubuntu-latest` con Docker Engine activo) | `≥ 85.00%` cobertura de líneas + 0 fallos | [Run CI/CD QA `develop` (#45728635298)](https://github.com/StyleRadar1/PT8080-Donations-Service/actions/runs/45728635298) · [Run CI PR #22 (#45728107219)](https://github.com/StyleRadar1/PT8080-Donations-Service/actions/runs/45728107219) · [Run CD PROD `main` (#45525360439)](https://github.com/StyleRadar1/PT8080-Donations-Service/actions/runs/45525360439) |
| **Cobertura de Líneas (JaCoCo `LINE`)** | **97.50%** (`2770 / 2841`) | **Local con Docker** (`mvn -B clean verify`, 95 clases analizadas) | `≥ 85.00%` *(umbral obligatorio del gate)* | Reporte `target/site/jacoco/jacoco.xml` · [`cobertura-jacoco-sprint3.png`](../evidencias/cobertura-jacoco-sprint3.png) |
| **Cobertura de Instrucciones (JaCoCo `INSTRUCTION`)** | **97.52%** (`12167 / 12477`) | **Local con Docker** (`mvn -B clean verify`, 95 clases analizadas) | Referencia (`≥ 85.00%`) | Reporte `target/site/jacoco/jacoco.xml` · [Run CI `develop` (#45728635298)](https://github.com/StyleRadar1/PT8080-Donations-Service/actions/runs/45728635298) |
| **Cobertura de Ramas (JaCoCo `BRANCH`)** | **88.24%** (`915 / 1037`) | **Local con Docker** (`mvn -B clean verify`, 95 clases analizadas) | Referencia (`≥ 85.00%`) | Reporte `target/site/jacoco/jacoco.xml` |
| **Cobertura de Métodos (JaCoCo `METHOD`)** | **95.83%** (`460 / 480`) | **Local con Docker** (`mvn -B clean verify`, 95 clases analizadas) | Referencia (`≥ 85.00%`) | Reporte `target/site/jacoco/jacoco.xml` |
| **Auditoría SonarQube** | **Quality Gate Passed (Rating A)** | **Análisis estático local / SonarQube** (0 Bugs, 0 Vulnerabilidades, 0 Code Smells críticos) | Quality Gate Aprobado | [`analisis-sonarqube.png`](../evidencias/analisis-sonarqube.png) |

---

## Pirámide de Pruebas Implementada

| Nivel de Prueba | Clases de Prueba Representativas | Alcance Verificado |
| :--- | :--- | :--- |
| **Dominio Puro (Unitarias)** | `SecondHandPublicationTest`, `DonationRequestTest`, `LocalidadBogotaTest`, `EnumsAndEventsTest` | Entidades, máquinas de estados, enums (`GarmentType`, `Size`, `Style`, `DeliveryMode`), invariantes de Bogotá y precio. |
| **Casos de Uso (Unitarias Mockito + E2E)** | `DonationCompleteFlowIntegrationTest`, `FoundationDeletedOutboxIntegrationTest`, `DonationDeliveryOutboxIntegrationTest`, `ManagePublicationUseCaseTest`, `CreateDonationRequestUseCaseTest`, `ClaimDonationUseCaseTest`, `GetPartnerFoundationsUseCaseTest`, `ConfirmDonationReceiptUseCaseTest`, `UploadGarmentPhotoUseCaseImplTest` | Los 10 casos de uso (`*UseCaseImpl`), los 7 escenarios del flujo benéfico completo, Outbox transaccional en cancelaciones y entregas (Notificaciones + Estadísticas independientes), concurrencia optimista y subida de imágenes. |
| **Controladores REST (`MockMvc`)** | `SecondHandPublicationControllerTest`, `DonationRequestControllerTest`, `PhotoUploadControllerTest`, `AuthControllerTest`, `HealthControllerTest`, `GlobalExceptionHandlerTest`, `CorrelationIdFilterTest` | Contratos HTTP (`GET /api/v1/donations` por rol incl. `ADMIN`, `deliveryProposal` + `donorId` resuelto por JWT), códigos 200/201/204, RFC 7807 sanitizado (`400`/`404`/`405`/`415`) y cabeceras `X-Correlation-Id` / `X-Request-Id`. |
| **Persistencia, Storage, HTTP y Mensajería** | `SecondHandPublicationPersistenceAdapterTest`, `DonationRequestPersistenceAdapterTest`, `TiendasFoundationCatalogAdapterTest`, `AzureBlobStorageAdapterTest`, `DonationEventPublisherAdapterTest`, `DonationRequestCreatedIntegrationEventTest`, `RabbitMQTlsCertificateTest` | Mapeo JPA, cliente HTTP hacia `ms-tiendas` (separación catálogo público vs canales autenticados + propagación MDC `X-Correlation-Id`), Azure Blob Storage con fallback local y RabbitMQ TLS (`AMQPS 5671`). |
| **Seguridad e Integración RBAC** | `SecurityRbacIntegrationTest`, `AuthQaSecurityIsolationTest`, `JwtTokenProviderTest`, `ResourceOwnershipValidatorTest`, `DemoUserServiceTest` | JWT reales HMAC-SHA256 en `GET /api/v1/donations` (`USER`, `STORE`, `FOUNDATION`, `ADMIN`, `401` sin token, intentos de suplantación y `404`/`503` desde `ms-tiendas`), aislamiento en QA/Prod y mitigación BOLA/IDOR. |

---

## Evidencias del Proyecto ([`docs/evidencias/`](../evidencias/))

📁 **Todas las evidencias de ejecución, pruebas, cobertura, calidad y despliegue se encuentran centralizadas en la carpeta [`docs/evidencias/`](../evidencias/)** (compilación Maven, reporte JaCoCo, auditoría SonarQube, Swagger UI, Health Check en Azure, contenedores Docker, RabbitMQ TLS y tablero Jira del squad).

---

[← Anterior: 8. Variables de Entorno](./08-variables-entorno.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 10. CI/CD y Pipelines →](./10-cicd-pipeline.md)
