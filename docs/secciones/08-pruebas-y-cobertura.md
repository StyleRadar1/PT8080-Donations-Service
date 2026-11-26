[← Anterior: 7. Variables de Entorno](./07-variables-de-entorno.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 9. CI/CD Pipelines →](./09-cicd-pipeline.md)

# 8. Pruebas y Cobertura (≥ 85% Líneas / ≥ 80% Ramas)

El proyecto exige una cobertura mínima de **líneas del `85.00%`** y de **ramas del `80.00%`** verificadas automáticamente mediante **JaCoCo** durante la fase `verify` (`./mvnw -B clean verify "-Dapi.version=1.44"`).

## Resumen de Métricas de Pruebas y Cobertura

| Métrica | Valor Alcanzado | Entorno y Condición | Umbral Exigido | Respaldo / Enlace de Ejecución |
| :--- | :---: | :--- | :---: | :--- |
| **Pruebas Automatizadas (Suite Completa con Docker/Testcontainers)** | **339 pruebas aprobadas** *(0 fallos, 0 errores, 0 omitidas en Surefire)* | **Local con Docker** (`./mvnw -B clean verify`, incluye PostgreSQL 16 en Testcontainers) | Suite completa sin fallos | [`339_test_run.png`](../evidencias/339_test_run.png) · [`docs/evidencias/`](../evidencias/) |
| **Pruebas Automatizadas (CI `develop` / `main`)** | **`develop` (`a96994a`) aprobado y desplegado en QA** · `main` (**277**) | **CI GitHub Actions** (`ubuntu-latest` con Docker Engine activo) | `≥ 85.00%` líneas / `≥ 80.00%` ramas + 0 fallos | [Run CI/CD QA `develop` (#45756632270)](https://github.com/StyleRadar1/PT8080-Donations-Service/actions/runs/45756632270) · [Run CD PROD `main` (#45525360439)](https://github.com/StyleRadar1/PT8080-Donations-Service/actions/runs/45525360439) |
| **Cobertura de Líneas (JaCoCo `LINE`)** | **97.47%** (`2769 / 2841`) | **Local con Docker** (`./mvnw -B clean verify`, 95 clases analizadas) | `≥ 85.00%` *(gate obligatorio)* | `target/site/jacoco/jacoco.xml` · [`jacoco-final.png`](../evidencias/jacoco-final.png) |
| **Cobertura de Instrucciones (JaCoCo `INSTRUCTION`)** | **97.50%** (`12171 / 12483`) | **Local con Docker** (`./mvnw -B clean verify`, 95 clases analizadas) | Referencia (`≥ 85.00%`) | `target/site/jacoco/jacoco.xml` |
| **Cobertura de Ramas (JaCoCo `BRANCH`)** | **88.09%** (`917 / 1041`) | **Local con Docker** (`./mvnw -B clean verify`, 95 clases analizadas) | `≥ 80.00%` *(gate obligatorio)* | `target/site/jacoco/jacoco.xml` · [`jacoco-final.png`](../evidencias/jacoco-final.png) |
| **Cobertura de Métodos (JaCoCo `METHOD`)** | **95.84%** (`461 / 481`) | **Local con Docker** (`./mvnw -B clean verify`, 95 clases analizadas) | Referencia | `target/site/jacoco/jacoco.xml` |
| **Auditoría SonarQube** | **Quality Gate Passed (Rating A)** | **Análisis estático local / SonarQube** (`0` Security, `0` Reliability, `0` Maintainability, `0` Hotspots, `93.3%` coverage, `0.2%` duplications) | Quality Gate Aprobado | [`sonarqube-final.png`](../evidencias/sonarqube-final.png) |

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

📁 **Todas las evidencias de ejecución, pruebas, cobertura, calidad y despliegue se encuentran centralizadas en [`docs/evidencias/`](../evidencias/):**

* **Ejecución Completa de Pruebas (`339` tests, `0` fallos):**

  ![Ejecución Final 339 Pruebas](../evidencias/339_test_run.png)

* **Reporte de Cobertura JaCoCo Final:**

  ![Reporte JaCoCo Final](../evidencias/jacoco-final.png)

* **Auditoría SonarQube Final (`Quality Gate Passed · 0 Issues`):**

  ![Auditoría SonarQube Final](../evidencias/sonarqube-final.png)

---

[← Anterior: 7. Variables de Entorno](./07-variables-de-entorno.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 9. CI/CD Pipelines →](./09-cicd-pipeline.md)
