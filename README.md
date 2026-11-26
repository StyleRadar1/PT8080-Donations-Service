<!-- ==============================================================================
  STYLE RADAR · README MAESTRO EJECUTIVO DE MICROSERVICIO
  Asignatura: DOSW (Diseño y Operación de Soluciones Web) · 2026
  Estándar obligatorio unificado para los microservicios del proyecto StyleRadar.
  Microservicio: PT8080-Donations-Service (ms-segunda-vida-donaciones)
  Squad Asignado: Portal8080 (PT8080)
============================================================================== -->

<div align="center">

  <a href="https://github.com/StyleRadar1">
    <img src="https://raw.githubusercontent.com/JUANNICOLASA/Frontend-Style-Radar/main/styleradar-frontend-v3/assets/logo/styleradar-logo.svg" width="340" alt="StyleRadar Logo Oficial" />
  </a>

  <h1>STYLE<font color="#5600EF">RADAR</font> &nbsp;·&nbsp; <code>ms-segunda-vida-donaciones</code></h1>

  <p align="center">
    <img src="https://img.shields.io/badge/BOUNDED_CONTEXT-Segunda_Vida_y_Donaciones-12063A?style=for-the-badge" alt="Context" />
    <img src="https://img.shields.io/badge/PUERTO_OFICIAL-8520-5600EF?style=for-the-badge" alt="Port" />
    <img src="https://img.shields.io/badge/ARQUITECTURA-HEXAGONAL-16A34A?style=for-the-badge" alt="Hexagonal" />
    <img src="https://img.shields.io/badge/SQUAD-PORTAL8080-12063A?style=for-the-badge" alt="Squad" />
  </p>

  <p align="center">
    <strong><font color="#5600EF">Descubre tu estilo. En tu ciudad.</font></strong><br />
    <em>Plataforma de moda local con inventario multi-sede geolocalizado en tiempo real, economía circular textil, vitrina benéfica y donaciones a fundaciones para Bogotá D.C.</em>
  </p>

  <p align="center">
    <a href="docs/secciones/03-endpoints-y-enlace-a-swagger.md"><img src="https://img.shields.io/badge/DEV_LOCAL-Active-16A34A?style=for-the-badge&logo=docker&logoColor=white" alt="DEV Local Active" /></a>
    <a href="docs/secciones/03-endpoints-y-enlace-a-swagger.md"><img src="https://img.shields.io/badge/QA_CLOUD-Active-5600EF?style=for-the-badge&logo=microsoftazure&logoColor=white" alt="QA Cloud Active" /></a>
    <a href="docs/secciones/03-endpoints-y-enlace-a-swagger.md"><img src="https://img.shields.io/badge/PROD_CLOUD-Active-16A34A?style=for-the-badge&logo=microsoftazure&logoColor=white" alt="PROD Cloud Active" /></a>
    <a href="https://github.com/StyleRadar1/PT8080-Donations-Service/actions/runs/45775635039"><img src="https://img.shields.io/badge/CI_PR_%2325_(%2345)-340_Tests_Passed-16A34A?style=for-the-badge&logo=githubactions&logoColor=white" alt="CI PR #25" /></a>
    <a href="https://github.com/StyleRadar1/PT8080-Donations-Service/actions/runs/45756632270"><img src="https://img.shields.io/badge/CD_QA_DEVELOP_(%2347)-aprobado-5600EF?style=for-the-badge&logo=githubactions&logoColor=white" alt="CD QA Develop Status" /></a>
    <a href="https://github.com/StyleRadar1/PT8080-Donations-Service/actions/runs/45525360439"><img src="https://img.shields.io/badge/CD_PROD_MAIN-277_Baseline_Passed-12063A?style=for-the-badge&logo=githubactions&logoColor=white" alt="CD PROD Main Status" /></a>
    <a href="docs/secciones/08-pruebas-y-cobertura.md"><img src="https://img.shields.io/badge/CI_Pruebas-340_aprobadas_(0_fallos)-16A34A?style=for-the-badge&logo=junit5&logoColor=white" alt="Pruebas Aprobadas en CI" /></a>
    <a href="docs/secciones/08-pruebas-y-cobertura.md"><img src="https://img.shields.io/badge/JaCoCo_Local-97.47%25_L%C3%ADneas_%7C_88.09%25_Ramas-16A34A?style=for-the-badge&logo=codecov&logoColor=white" alt="Cobertura JaCoCo Local" /></a>
    <a href="https://sonarcloud.io/summary/new_code?id=StyleRadar1_PT8080-Donations-Service&pullRequest=25"><img src="https://img.shields.io/badge/SonarCloud_PR_%2325-Quality_Gate_Passed_(0_Issues)-16A34A?style=for-the-badge&logo=sonarcloud&logoColor=white" alt="SonarCloud Passed" /></a>
  </p>

  <h3>Tecnologías y Dependencias Principales</h3>

  <p align="center">
    <a href="docs/secciones/05-tecnologias-y-dependencias.md">
      <img src="https://img.shields.io/badge/JAVA-21__LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21" />
    </a>
    <a href="docs/secciones/05-tecnologias-y-dependencias.md">
      <img src="https://img.shields.io/badge/SPRING__BOOT-3.3.4-12063A?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot 3.3.4" />
    </a>
    <a href="docs/secciones/05-tecnologias-y-dependencias.md">
      <img src="https://img.shields.io/badge/POSTGRESQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL 16" />
    </a>
    <a href="docs/secciones/05-tecnologias-y-dependencias.md">
      <img src="https://img.shields.io/badge/FLYWAY-10.10.0-CC0200?style=for-the-badge&logo=flyway&logoColor=white" alt="Flyway" />
    </a>
    <a href="docs/secciones/05-tecnologias-y-dependencias.md">
      <img src="https://img.shields.io/badge/AZURE__BLOB-12.28.0-0078D4?style=for-the-badge&logo=microsoftazure&logoColor=white" alt="Azure Blob Storage" />
    </a>
    <a href="docs/secciones/05-tecnologias-y-dependencias.md">
      <img src="https://img.shields.io/badge/RABBITMQ-3.12_TLS-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white" alt="RabbitMQ 3.12 TLS" />
    </a>
    <a href="docs/secciones/05-tecnologias-y-dependencias.md">
      <img src="https://img.shields.io/badge/SPRING_SECURITY-JJWT_0.13.0-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" alt="Spring Security & JJWT" />
    </a>
    <a href="docs/secciones/05-tecnologias-y-dependencias.md">
      <img src="https://img.shields.io/badge/OPENAPI-SpringDoc_2.5.0-85EA2D?style=for-the-badge&logo=openapiinitiative&logoColor=black" alt="SpringDoc OpenAPI" />
    </a>
  </p>

</div>

---

## <img src="https://img.shields.io/badge/ÍNDICE-5600EF?style=for-the-badge" height="20" align="absmiddle" /> ÍNDICE DE CONTENIDOS (12 SECCIONES)

| # | Sección | Documentación Detallada |
| :---: | :--- | :--- |
| **1** | **Nombre del servicio y su bounded context** | [01-nombre-del-servicio-y-bounded-context.md](docs/secciones/01-nombre-del-servicio-y-bounded-context.md) |
| **2** | **Arquitectura hexagonal con sus diagramas** | [02-arquitectura-hexagonal-y-diagramas.md](docs/secciones/02-arquitectura-hexagonal-y-diagramas.md) |
| **3** | **Endpoints y enlace a Swagger** | [03-endpoints-y-enlace-a-swagger.md](docs/secciones/03-endpoints-y-enlace-a-swagger.md) |
| **4** | **Eventos que publica y consume (RabbitMQ)** | [04-eventos-que-publica-y-consume.md](docs/secciones/04-eventos-que-publica-y-consume.md) |
| **5** | **Tecnologías y dependencias** | [05-tecnologias-y-dependencias.md](docs/secciones/05-tecnologias-y-dependencias.md) |
| **6** | **Cómo ejecutar local y con Docker** | [06-como-ejecutar-local-y-docker.md](docs/secciones/06-como-ejecutar-local-y-docker.md) |
| **7** | **Variables de entorno** | [07-variables-de-entorno.md](docs/secciones/07-variables-de-entorno.md) |
| **8** | **Pruebas y cobertura (≥ 85% Lineas / ≥ 80% Ramas)** | [08-pruebas-y-cobertura.md](docs/secciones/08-pruebas-y-cobertura.md) |
| **9** | **CI/CD con el badge del pipeline** | [09-cicd-pipeline.md](docs/secciones/09-cicd-pipeline.md) |
| **10** | **Ramas y cómo contribuir** | [10-ramas-y-como-contribuir.md](docs/secciones/10-ramas-y-como-contribuir.md) |
| **11** | **Seguridad, RBAC y análisis OWASP Top 10** | [11-seguridad-y-owasp.md](docs/secciones/11-seguridad-y-owasp.md) |
| **12** | **Equipo, responsables y enlace a la épica en Jira** | [12-equipo-responsables-y-epica-jira.md](docs/secciones/12-equipo-responsables-y-epica-jira.md) |
