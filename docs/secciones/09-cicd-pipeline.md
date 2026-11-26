[← Anterior: 8. Pruebas y Cobertura](./08-pruebas-y-cobertura.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 10. Ramas y Cómo Contribuir →](./10-ramas-y-como-contribuir.md)

# 9. CI/CD con el Badge del Pipeline

El ciclo de integración y despliegue continuo está automatizado en **GitHub Actions** siguiendo el flujo **`develop → main`** mediante dos workflows (`.github/workflows/ci-qa.yml` para QA en `develop` y `.github/workflows/cd-prod.yml` para Producción en `main`).

## Matriz de Pipelines por Rama Protegida

| Pipeline | Rama / Entorno | Trigger | Quality Gate Requerido | Ejecución y Estado |
| :--- | :---: | :---: | :--- | :---: |
| **CI - PR #22 (`fix/auditoria-integral`)** | `fix/auditoria-integral` → `develop` | Merge PR #22 (`5f896e9`) | **Build, pruebas con Docker y JaCoCo aprobados en CI** (rama actual: **339 pruebas aprobadas**, `97.50%` Líneas / `88.24%` Ramas JaCoCo) | [![CI PR #22](https://img.shields.io/badge/PR_%2322-aprobado-16A34A?style=flat-square&logo=githubactions&logoColor=white)](https://github.com/StyleRadar1/PT8080-Donations-Service/actions/runs/45728107219) |
| **CI - Develop** | `develop`<br>*(Staging QA)* | Push / PR hacia `develop` | **339 pruebas aprobadas** *(0 fallos, 0 omitidas con Docker)* + JaCoCo `97.50%` Líneas / `88.24%` Ramas (`≥ 85%`) + Deploy QA `4a2cbec` verificado | [![CI Develop](https://img.shields.io/badge/develop-aprobado-16A34A?style=flat-square&logo=githubactions&logoColor=white)](https://github.com/StyleRadar1/PT8080-Donations-Service/actions/runs/45728635298) |
| **CI - Main** | `main`<br>*(Producción)* | Push / PR (`develop → main`) | **339 pruebas aprobadas** *(baseline `main` en CI con Docker: 277)* + JaCoCo `97.50%` Líneas / `88.24%` Ramas (`≥ 85%`) + Docker Build | [![CI Main](https://img.shields.io/badge/main-aprobado-16A34A?style=flat-square&logo=githubactions&logoColor=white)](https://github.com/StyleRadar1/PT8080-Donations-Service/actions/runs/45525360439) |
| **CD - Despliegue QA** | Azure QA (`rg-styleradar`) | Push / Merge a `develop` | Deploy Web App + Smoke Test `/api/v1/health` | [![Deploy QA](https://img.shields.io/badge/Deploy_QA-Azure_Active-5600EF?style=flat-square&logo=microsoftazure&logoColor=white)](https://styleradar-donaciones-qa-jmunar.azurewebsites.net/api/v1/health) |
| **CD - Despliegue PROD** | Azure PROD (`rg-styleradar`) | Push / Merge `develop → main` (CMV) | Aprobación CMV + Smoke Test `/api/v1/health` | [![Deploy PROD](https://img.shields.io/badge/Deploy_PROD-Azure_Active-16A34A?style=flat-square&logo=microsoftazure&logoColor=white)](https://styleradar-donaciones-prod-jmunar.azurewebsites.net/api/v1/health) |

> **Nota de flujo:** La rama productiva oficial del equipo es `main` (cierre **`develop → main`**). El workflow `.github/workflows/cd-prod.yml` valida el endpoint `/api/v1/health` tras cada despliegue.

```mermaid
flowchart LR
    A["Push / PR<br>(GitHub)"] --> B["Build & Compile<br>(Maven Java 21)"]
    B --> C["339 Pruebas Aprobadas & JaCoCo<br>(97.50% Líneas · 88.24% Ramas ≥ 85%)"]
    C --> D["Empaquetado JAR &<br>Docker OCI Image"]
    D -->|Rama develop| E["Deploy Automático QA<br>(Smoke Test /api/v1/health)"]
    D -->|Cierre develop → main + CMV| F["Deploy Producción PROD<br>(Smoke Test /api/v1/health)"]

    classDef step fill:#12063A,stroke:#5600EF,stroke-width:2px,color:#FFFFFF;
    classDef pass fill:#DCFCE7,stroke:#16A34A,stroke-width:2px,color:#0C0C12;

    class A,B,D step;
    class C,E,F pass;
```


### Pipelines Homologados y Compuertas Automáticas

| Workflow | Trigger | Validaciones Automatizadas |
| :--- | :--- | :--- |
| **CI PR (`ci.yml`)** | `push` y `pull_request` hacia `main`, `master`, `develop` | `./mvnw -B clean verify`, JaCoCo (`LINE >= 85%` y `BRANCH >= 80%`), Quality Gate SonarQube/SonarCloud (`-Dsonar.qualitygate.wait=true`) y validación de build Docker. |
| **CD QA (`ci-qa.yml`)** | `push` a `develop` / `workflow_dispatch` | Verificación Maven Wrapper, Quality Gate Sonar, empaquetado JAR, despliegue a Azure QA y Smoke Test en vivo validando `status=UP`, `commitSha`, `/api/v1/v3/api-docs` (`200`) y `/api/v1/swagger-ui/index.html` (`200`). |
| **CD PROD (`cd-prod.yml`)** | `push` a `main`/`master` / `workflow_dispatch` | Verificación Maven Wrapper, Quality Gate Sonar, aprobación productiva, despliegue Azure PROD y Smoke Test validando `status=UP`, `commitSha`, `/api/v1/v3/api-docs` (`200`) y `/api/v1/swagger-ui/index.html` (`200`). |

### Evidencia del Pipeline en GitHub Actions

<div align="center">
  <img src="../evidencias/pipeline-ci-cd.png" alt="Evidencia del Pipeline CI/CD en GitHub Actions" width="100%" />
  <p><sub><b>Figura 9.1:</b> Ejecución exitosa en GitHub Actions del pipeline CI/CD sobre <code>develop</code> (<code>6m 39s</code>, Build + Tests + Cobertura + Despliegue verificado en Azure QA).</sub></p>
</div>

---

[← Anterior: 8. Pruebas y Cobertura](./08-pruebas-y-cobertura.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 10. Ramas y Cómo Contribuir →](./10-ramas-y-como-contribuir.md)
