[← Anterior: 8. Pruebas y Cobertura](./08-pruebas-y-cobertura.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 10. Ramas y Cómo Contribuir →](./10-ramas-y-como-contribuir.md)

# 9. CI/CD con el Badge del Pipeline

El ciclo de integración y despliegue continuo está automatizado en **GitHub Actions** mediante tres workflows independientes: `.github/workflows/ci.yml` (revisión de PRs y ramas), `.github/workflows/ci-qa.yml` (despliegue QA en `develop`) y `.github/workflows/cd-prod.yml` (despliegue PROD en `main`).

<p align="center">
  <a href="https://github.com/StyleRadar1/PT8080-Donations-Service/actions/runs/45775635039"><img src="https://img.shields.io/badge/CI_PR_%2325_(%2345)-passing-16A34A?style=for-the-badge&logo=githubactions&logoColor=white" alt="CI PR #25" /></a>
  <a href="https://github.com/StyleRadar1/PT8080-Donations-Service/actions/runs/45756632270"><img src="https://img.shields.io/badge/CD_QA_DEVELOP_(%2347)-passing-5600EF?style=for-the-badge&logo=githubactions&logoColor=white" alt="CD QA Develop" /></a>
  <a href="https://github.com/StyleRadar1/PT8080-Donations-Service/actions/runs/45525360439"><img src="https://img.shields.io/badge/CD_PROD_MAIN-passing-12063A?style=for-the-badge&logo=githubactions&logoColor=white" alt="CD PROD Main" /></a>
  <a href="https://sonarcloud.io/summary/new_code?id=StyleRadar1_PT8080-Donations-Service&pullRequest=25"><img src="https://img.shields.io/badge/SonarCloud_PR_%2325-Passed_(0_Issues)-16A34A?style=for-the-badge&logo=sonarcloud&logoColor=white" alt="Sonar Gate" /></a>
</p>

## Matriz de Pipelines por Rama Protegida

| Pipeline | Rama / Entorno | Trigger | Alcance Verificado (CI vs. Local) | Ejecución y Estado |
| :--- | :---: | :--- | :--- | :---: |
| **CI - PR #25 (`ci.yml`)** | `feat/estandarizacion-final-cadche` → `develop` | `push` / `pull_request` / `workflow_dispatch` | **CI (`#45`, `b1b21c7`):** `340` pruebas aprobadas (`0` fallos, `0` omitidas), JaCoCo (`LINE >= 85%`, `BRANCH >= 80%`), **SonarCloud Quality Gate `OK` (`0 Issues`)** y Docker Build OCI.<br>**Local (`jacoco.xml`):** `97.47%` líneas y `88.09%` ramas (`95` clases). | [![CI PR #25](https://img.shields.io/badge/CI_%2345-aprobado-16A34A?style=flat-square&logo=githubactions&logoColor=white)](https://github.com/StyleRadar1/PT8080-Donations-Service/actions/runs/45775635039) |
| **CD - Develop (`ci-qa.yml`)** | `develop`<br>*(Staging QA)* | `push` a `develop` o `workflow_dispatch` | **CI (`#47`, `a96994a`):** Build + suite + JaCoCo + despliegue Azure QA + Smoke Test (`/api/v1/health`, `/api/v1/v3/api-docs` y `/api/v1/swagger-ui/index.html`). | [![CD QA #47](https://img.shields.io/badge/develop_%2347-aprobado-16A34A?style=flat-square&logo=githubactions&logoColor=white)](https://github.com/StyleRadar1/PT8080-Donations-Service/actions/runs/45756632270) |
| **CD - Main (`cd-prod.yml`)** | `main`<br>*(Producción)* | `push` a `main`/`master` o `workflow_dispatch` | **CI:** `277` pruebas aprobadas en baseline `main` + despliegue Azure PROD + Smoke Test (`/api/v1/health` UP + verificación de Swagger deshabilitado en `401`/`403`/`404`). | [![CD PROD](https://img.shields.io/badge/main-aprobado-16A34A?style=flat-square&logo=githubactions&logoColor=white)](https://github.com/StyleRadar1/PT8080-Donations-Service/actions/runs/45525360439) |
| **CD - Salud en Vivo QA / PROD** | Azure App Service (`rg-styleradar`) | Post-despliegue automático | **QA:** `/api/v1/health` + Swagger habilitados.<br>**PROD:** `/api/v1/health` activo; Swagger restringido (`springdoc.*.enabled=false`, HTTP `401`/`403`/`404`). | [![QA Health](https://img.shields.io/badge/QA_Health-UP-5600EF?style=flat-square&logo=microsoftazure&logoColor=white)](https://styleradar-donaciones-qa-jmunar.azurewebsites.net/api/v1/health) [![PROD Health](https://img.shields.io/badge/PROD_Health-UP-16A34A?style=flat-square&logo=microsoftazure&logoColor=white)](https://styleradar-donaciones-prod-jmunar.azurewebsites.net/api/v1/health) |

> **Nota sobre SonarQube/SonarCloud en CI:** Los tres workflows exigen `SONAR_TOKEN` configurado en GitHub Repository Secrets (org `styleradar1`, projectKey `StyleRadar1_PT8080-Donations-Service` en `https://sonarcloud.io`), fallando explícitamente en ramas oficiales (`develop`, `main`, `master`) y PRs internos si el secreto falta, sin omisiones silenciosas. En el run `#45` de `ci.yml`, SonarCloud se ejecutó en CI aprobando el Quality Gate con `0` bugs, `0` vulnerabilidades y `0` code smells.

```mermaid
flowchart LR
    A["Push / PR / workflow_dispatch<br>(GitHub)"] --> B["Maven Wrapper (./mvnw)<br>JDK 21 Temurin"]
    B --> C["339 Pruebas & JaCoCo Gate<br>(CI >= 85% Line / >= 80% Branch)<br>(Local: 97.47% L / 88.09% R)"]
    C --> D["SonarCloud Quality Gate<br>(-Dsonar.qualitygate.wait=true)"]
    D --> E["Empaquetado JAR &<br>Docker OCI Check"]
    E -->|develop push / dispatch| F["Deploy Azure QA<br>Smoke: Health + Swagger 200"]
    E -->|main push / dispatch| G["Deploy Azure PROD<br>Smoke: Health + Swagger Off"]

    classDef step fill:#12063A,stroke:#5600EF,stroke-width:2px,color:#FFFFFF;
    classDef pass fill:#DCFCE7,stroke:#16A34A,stroke-width:2px,color:#0C0C12;

    class A,B,D,E step;
    class C,F,G pass;
```

### Detalle de los Workflows Configurados

| Workflow | Trigger | Validaciones Automatizadas |
| :--- | :--- | :--- |
| **CI PR (`ci.yml`)** | `push` y `pull_request` hacia `main`, `master`, `develop` + `workflow_dispatch` | `./mvnw -B clean verify`, JaCoCo (`LINE >= 85%` y `BRANCH >= 80%`), Quality Gate SonarCloud obligatorio (`SONAR_TOKEN` requerido) y validación de build Docker (`docker/build-push-action`). |
| **CD QA (`ci-qa.yml`)** | `push` a `develop` y `workflow_dispatch` en `develop` | Verificación Maven Wrapper, Quality Gate Sonar obligatorio, empaquetado JAR, despliegue Azure QA (`push` y `workflow_dispatch`) y Smoke Test validando `status=UP`, `commitSha`, `/api/v1/v3/api-docs` (`200`) y `/api/v1/swagger-ui/index.html` (`200`). |
| **CD PROD (`cd-prod.yml`)** | `push` a `main`/`master` y `workflow_dispatch` | Verificación Maven Wrapper, Quality Gate Sonar obligatorio, aprobación productiva, despliegue Azure PROD y Smoke Test validando `status=UP`, `commitSha` y Swagger deshabilitado (`401`/`403`/`404` por `OWASP A05`). |

### Evidencia del Pipeline en GitHub Actions

<div align="center">
  <img src="../evidencias/pipeline-ci-cd.png" alt="Evidencia del Pipeline CI/CD en GitHub Actions" width="100%" />
  <p><sub><b>Figura 9.1:</b> Ejecución exitosa en GitHub Actions del pipeline CI/CD sobre <code>develop</code> (Run <code>#47</code> / <code>45756632270</code>, <code>6m 39s</code>, Build + Tests + Cobertura + Despliegue verificado en Azure QA). El último run de PR con SonarCloud Gate aprobado (<code>ci.yml</code> <code>#45</code>) corresponde a <a href="https://github.com/StyleRadar1/PT8080-Donations-Service/actions/runs/45775635039"><code>45775635039</code></a>.</sub></p>
</div>

---

[← Anterior: 8. Pruebas y Cobertura](./08-pruebas-y-cobertura.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 10. Ramas y Cómo Contribuir →](./10-ramas-y-como-contribuir.md)
