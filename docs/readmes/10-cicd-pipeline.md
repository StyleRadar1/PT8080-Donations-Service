[← Anterior: 9. Pruebas y Cobertura](./09-pruebas-cobertura.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 11. Ramas y Cómo Contribuir →](./11-ramas-contribuir.md)

# 10. CI/CD con el Badge del Pipeline

El ciclo de integración y despliegue continuo está automatizado en **GitHub Actions** siguiendo el flujo **`develop → main`** mediante dos workflows (`.github/workflows/ci-qa.yml` para QA en `develop` y `.github/workflows/cd-prod.yml` para Producción en `main`).

## Matriz de Pipelines por Rama Protegida

| Pipeline | Rama / Entorno | Trigger | Quality Gate Requerido | Ejecución y Estado |
| :--- | :---: | :---: | :--- | :---: |
| **CI - Develop** | `develop`<br>*(Staging QA)* | Push / PR hacia `develop` | **279 pruebas totales en suite** *(278 aprobadas en local sin Docker; último baseline en CI: 277)* + JaCoCo Líneas `≥ 85%` (`98.21%` en local) + Docker Build | [![CI Develop](https://img.shields.io/badge/develop-aprobado-16A34A?style=flat-square&logo=githubactions&logoColor=white)](https://github.com/StyleRadar1/PT8080-Donations-Service/actions/runs/44974140792) |
| **CI - Main** | `main`<br>*(Producción)* | Push / PR (`develop → main`) | **279 pruebas totales en suite** *(último baseline en CI `main`: 277)* + JaCoCo Líneas `≥ 85%` (`98.21%` en local) + Docker Build | [![CI Main](https://img.shields.io/badge/main-aprobado-16A34A?style=flat-square&logo=githubactions&logoColor=white)](https://github.com/StyleRadar1/PT8080-Donations-Service/actions/runs/45525360439) |
| **CD - Despliegue QA** | Azure QA (`rg-styleradar`) | Push / Merge a `develop` | Deploy Web App + Smoke Test `/api/v1/health` | [![Deploy QA](https://img.shields.io/badge/Deploy_QA-Azure_Active-5600EF?style=flat-square&logo=microsoftazure&logoColor=white)](https://styleradar-donaciones-qa-jmunar.azurewebsites.net/api/v1/health) |
| **CD - Despliegue PROD** | Azure PROD (`rg-styleradar`) | Push / Merge `develop → main` (CMV) | Aprobación CMV + Smoke Test `/api/v1/health` | [![Deploy PROD](https://img.shields.io/badge/Deploy_PROD-Azure_Active-16A34A?style=flat-square&logo=microsoftazure&logoColor=white)](https://styleradar-donaciones-prod-jmunar.azurewebsites.net/api/v1/health) |

> **Nota de flujo:** La rama productiva oficial del equipo es `main` (cierre **`develop → main`**). El workflow `.github/workflows/cd-prod.yml` valida el endpoint `/api/v1/health` tras cada despliegue.

```mermaid
flowchart LR
    A["Push / PR<br>(GitHub)"] --> B["Build & Compile<br>(Maven Java 21)"]
    B --> C["279 Pruebas & JaCoCo<br>(98.21% Líneas en local ≥ 85%)"]
    C --> D["Empaquetado JAR &<br>Docker OCI Image"]
    D -->|Rama develop| E["Deploy Automático QA<br>(Smoke Test /api/v1/health)"]
    D -->|Cierre develop → main + CMV| F["Deploy Producción PROD<br>(Smoke Test /api/v1/health)"]

    classDef step fill:#12063A,stroke:#5600EF,stroke-width:2px,color:#FFFFFF;
    classDef pass fill:#DCFCE7,stroke:#16A34A,stroke-width:2px,color:#0C0C12;

    class A,B,D step;
    class C,E,F pass;
```

---

[← Anterior: 9. Pruebas y Cobertura](./09-pruebas-cobertura.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 11. Ramas y Cómo Contribuir →](./11-ramas-contribuir.md)
