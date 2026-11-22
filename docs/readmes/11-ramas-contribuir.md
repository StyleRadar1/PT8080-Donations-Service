[← Anterior: 10. CI/CD y Pipelines](./10-cicd-pipeline.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 12. Épica en Jira y Requerimientos →](./12-epica-jira-requerimientos.md)

# 11. Ramas y Cómo Contribuir

El equipo adopta el modelo **GitFlow adaptado para microservicios**. Las ramas `master` y `develop` están protegidas y reciben cambios exclusivamente mediante Pull Requests:

```text
master / main                           <-- Producción (Azure PROD · Requiere CMV)
└── develop                             <-- Integración continua (Azure QA)
    ├── feature/nombre-ejemplo          <-- Agregar nuevas funcionalidades
    └── fix/nombre-ejemplo              <-- Arreglar bugs solamente
```

## Convención de Commits (*Conventional Commits*)

```text
<tipo>(<alcance>): <descripción imperativa en español> [#PT8080-XX]
```

## Checklist Obligatorio para Pull Requests

- [x] La rama parte de la versión más reciente de `develop`.
- [x] El código compila limpiamente sin advertencias críticas con `mvn clean compile`.
- [x] Todas las pruebas automatizadas pasan en verde (`mvn -B clean verify "-Dapi.version=1.44"` con **276 pasando en local**, 0 fallos, 0 errores, 1 condicional PostgreSQL CI omitida).
- [x] La cobertura en JaCoCo supera el 85% obligatorio (alcanzado **98.20% líneas / 98.07% instrucciones / 88.80% ramas / 96.30% métodos**).
- [x] Diagramas de arquitectura actualizados en [`docs/diagramas/drawio`](../diagramas/drawio) y [`docs/diagramas/png`](../diagramas/png).
- [x] Evidencias de pruebas, cobertura JaCoCo, SonarQube, Swagger y despliegue almacenadas en [`docs/evidencias/`](../evidencias/).
- [x] Colección Postman y pruebas automatizadas de todos los endpoints disponibles en [`docs/postman/`](../postman/).
- [x] No se introducen credenciales ni secretos en el código (uso de variables de entorno).
- [x] El PR cuenta con la revisión y aprobación del líder técnico del squad.

---

[← Anterior: 10. CI/CD y Pipelines](./10-cicd-pipeline.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 12. Épica en Jira y Requerimientos →](./12-epica-jira-requerimientos.md)
