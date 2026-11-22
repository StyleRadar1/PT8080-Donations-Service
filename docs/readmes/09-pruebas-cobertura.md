[← Anterior: 8. Variables de Entorno](./08-variables-entorno.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 10. CI/CD y Pipelines →](./10-cicd-pipeline.md)

# 9. Pruebas y Cobertura (≥ 85%)

El proyecto exige una cobertura de código mínima del **85%** verificada automáticamente mediante **JaCoCo** durante la fase `verify` de Maven (`mvn -B clean verify "-Dapi.version=1.44"`).

## Resumen de Métricas de Pruebas y Cobertura

| Métrica | Valor Alcanzado | Umbral Exigido | Estado |
| :--- | :---: | :---: | :---: |
| **Pruebas Automatizadas Ejecutadas** | **276 pasando** *(277 totales: 0 fallos, 0 errores, 1 prueba PostgreSQL condicional CI omitida en local: `VitrinaAbiertaPostgresConcurrencyIntegrationTest`)* | Suite completa | ✅ **0 fallos, 0 errores** |
| **Cobertura de Instrucciones (JaCoCo)** | **98.07%** (`9763 / 9955`) | `≥ 85.00%` | ✅ **Aprobado (+13.07%)** |
| **Cobertura de Líneas (JaCoCo)** | **98.20%** (`2177 / 2217`) | `≥ 85.00%` | ✅ **Aprobado (+13.20%)** |
| **Cobertura de Ramas (JaCoCo)** | **88.80%** (`666 / 750`) | `≥ 85.00%` | ✅ **Aprobado (+3.80%)** |
| **Cobertura de Métodos (JaCoCo)** | **96.30%** (`364 / 378`) | `≥ 85.00%` | ✅ **Aprobado (+11.30%)** |
| **Auditoría SonarQube** | **Quality Gate Passed (Rating A)** | 0 Bugs, 0 Vuln, 0 Smells | ✅ **Calificación A** |

---

## Pirámide de Pruebas Implementada

| Nivel de Prueba | Clases de Prueba Representativas | Alcance Verificado |
| :--- | :--- | :--- |
| **Dominio Puro (Unitarias)** | `SecondHandPublicationTest`, `DonationRequestTest`, `LocalidadBogotaTest`, `EnumsAndEventsTest` | Entidades, máquinas de estados, enums (`GarmentType`, `Size`, `Style`, `DeliveryMode`), invariantes de Bogotá y precio. |
| **Casos de Uso (Unitarias Mockito + E2E)** | `DonationCompleteFlowIntegrationTest`, `ManagePublicationUseCaseTest`, `CreateDonationRequestUseCaseTest`, `ClaimDonationUseCaseTest`, `GetPartnerFoundationsUseCaseTest`, `UploadGarmentPhotoUseCaseImplTest` | Los 10 casos de uso (`*UseCaseImpl`), los 7 escenarios del flujo benéfico completo, concurrencia optimista y subida de imágenes. |
| **Controladores REST (`MockMvc`)** | `SecondHandPublicationControllerTest`, `DonationRequestControllerTest`, `PhotoUploadControllerTest`, `AuthControllerTest`, `HealthControllerTest` | Contratos HTTP (`deliveryProposal` + `donorId` resuelto por JWT, rechazo de `interesadosIds` desde cliente), códigos 200/201/204 y RFC 7807 (`ProblemDetail`) para 400/401/403/404/409/500/503. |
| **Persistencia, Storage, HTTP y Mensajería** | `SecondHandPublicationPersistenceAdapterTest`, `DonationRequestPersistenceAdapterTest`, `TiendasFoundationCatalogAdapterTest`, `AzureBlobStorageAdapterTest`, `DonationEventPublisherAdapterTest`, `DonationRequestCreatedIntegrationEventTest`, `RabbitMQTlsCertificateTest` | Mapeo JPA, cliente HTTP hacia `ms-tiendas`, Azure Blob Storage con fallback local, fechas ISO-8601 sin zona (`yyyy-MM-dd'T'HH:mm:ss`) y RabbitMQ TLS (`AMQPS 5671`). |
| **Seguridad e Integración RBAC** | `SecurityRbacIntegrationTest`, `AuthQaSecurityIsolationTest`, `JwtTokenProviderTest`, `ResourceOwnershipValidatorTest`, `DemoUserServiceTest` | JWT oficiales `ms-auth` (`sub` + `roles`), aislamiento en QA/Prod, roles (`USER`, `STORE`, `FOUNDATION`, `ADMIN`), mitigación BOLA/IDOR, CORS y `representanteId`. |

---

## Evidencias del Proyecto ([`docs/evidencias/`](../evidencias/))

📁 **Todas las evidencias de ejecución, pruebas, cobertura, calidad y despliegue se encuentran centralizadas en la carpeta [`docs/evidencias/`](../evidencias/)** (compilación Maven, reporte JaCoCo, auditoría SonarQube, Swagger UI, Health Check en Azure, contenedores Docker, RabbitMQ TLS y tablero Jira del squad).

---

[← Anterior: 8. Variables de Entorno](./08-variables-entorno.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 10. CI/CD y Pipelines →](./10-cicd-pipeline.md)
