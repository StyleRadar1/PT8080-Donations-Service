[← Anterior: 9. Pruebas y Cobertura](./09-pruebas-cobertura.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 11. Ramas y Cómo Contribuir →](./11-ramas-contribuir.md)

# 10. CI/CD con el Badge del Pipeline

El ciclo de integración y despliegue continuo está automatizado en **GitHub Actions** mediante dos flujos separados (`.github/workflows/ci-qa.yml` para QA y `.github/workflows/cd-prod.yml` para Producción):

## Matriz de Pipelines por Rama Protegida

| Pipeline | Rama / Entorno | Trigger | Quality Gate Requerido | Estado |
| :--- | :---: | :---: | :--- | :---: |
| **CI - Develop** | `develop`<br>*(Staging QA)* | Push / PR | 275 Tests + JaCoCo ≥ 85% + Docker Build | [![CI Develop](https://img.shields.io/badge/develop-passing-16A34A?style=flat-square&logo=githubactions&logoColor=white)](https://github.com/StyleRadar1/PT8080-Donations-Service/actions) |
| **CI - Master** | `master` / `main`<br>*(Producción)* | Push / PR | 275 Tests + JaCoCo ≥ 85% + Docker Build | [![CI Master](https://img.shields.io/badge/master-passing-16A34A?style=flat-square&logo=githubactions&logoColor=white)](https://github.com/StyleRadar1/PT8080-Donations-Service/actions) |
| **CD - Despliegue QA** | Azure QA (`rg-styleradar`) | Push / Merge a `develop` | Deploy Web App + Smoke Test `/health` | [![Deploy QA](https://img.shields.io/badge/Deploy_QA-Azure_Active-5600EF?style=flat-square&logo=microsoftazure&logoColor=white)](https://styleradar-donaciones-qa-jmunar.azurewebsites.net/api/v1/swagger-ui/index.html) |
| **CD - Despliegue PROD** | Azure PROD (`rg-styleradar`) | Push / Merge a `master` (CMV) | Aprobación CMV + Smoke Test `/health` | [![Deploy PROD](https://img.shields.io/badge/Deploy_PROD-Azure_Active-16A34A?style=flat-square&logo=microsoftazure&logoColor=white)](https://styleradar-donaciones-prod-jmunar.azurewebsites.net/api/v1/swagger-ui/index.html) |

```mermaid
flowchart LR
    A["Push / PR<br>(GitHub)"] --> B["Build & Compile<br>(Maven Java 21)"]
    B --> C["275 Tests & JaCoCo<br>(98.25% ≥ 85%)"]
    C --> D["Empaquetado JAR &<br>Docker OCI Image"]
    D -->|Rama develop| E["Deploy Automático QA<br>(Azure App Service QA)"]
    D -->|Rama main + CMV| F["Deploy Producción PROD<br>(Azure App Service PROD)"]

    classDef step fill:#12063A,stroke:#5600EF,stroke-width:2px,color:#FFFFFF;
    classDef pass fill:#DCFCE7,stroke:#16A34A,stroke-width:2px,color:#0C0C12;

    class A,B,D step;
    class C,E,F pass;
```

---

[← Anterior: 9. Pruebas y Cobertura](./09-pruebas-cobertura.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 11. Ramas y Cómo Contribuir →](./11-ramas-contribuir.md)
