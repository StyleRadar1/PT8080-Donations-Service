[← Anterior: 10. CI/CD y Pipelines](./10-cicd-pipeline.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 12. Épica en Jira y Requerimientos →](./12-epica-jira-requerimientos.md)

# 11. Ramas y Cómo Contribuir

El equipo adopta el modelo **GitFlow adaptado para microservicios** con flujo de cierre **`develop → main`**. Las ramas `main` y `develop` están protegidas y reciben cambios exclusivamente mediante Pull Requests:

```text
main                                    <-- Producción (Azure PROD · Cierre develop → main · Requiere CMV)
└── develop                             <-- Integración continua (Azure QA)
    ├── feature/nombre-ejemplo          <-- Agregar nuevas funcionalidades
    ├── fix/nombre-ejemplo              <-- Arreglar bugs solamente
    └── docs/nombre-ejemplo             <-- Actualizaciones de documentación
```

## Convención de Commits (*Conventional Commits*)

```text
<tipo>(<alcance>): <descripción imperativa en español> [#PT8080-XX]
```

## Checklist Obligatorio para Pull Requests

- [x] La rama parte de la versión más reciente de `develop`.
- [x] El código compila limpiamente sin advertencias críticas con `mvn clean compile`.
- [x] Todas las pruebas automatizadas resultan aprobadas (`mvn -B clean verify` con **330 pruebas aprobadas en entorno local** de 331 en Surefire —0 fallos, 0 errores, 1 clase condicional PostgreSQL omitida sin Docker—; último PR #22 `5f896e9` aprobado en CI con Docker: [Run CI PR #22 (#45728107219)](https://github.com/StyleRadar1/PT8080-Donations-Service/actions/runs/45728107219); último `develop` `cee0037`: [Run CI `develop` (#45548074249)](https://github.com/StyleRadar1/PT8080-Donations-Service/actions/runs/45548074249)).
- [x] La cobertura en JaCoCo supera el `85.00%` obligatorio de líneas (alcanzado en entorno local: **96.62% líneas / 96.49% instrucciones / 84.02% ramas / 95.62% métodos**, detallado en [`09-pruebas-cobertura.md`](./09-pruebas-cobertura.md)).
- [x] Diagramas de arquitectura actualizados en [`docs/diagramas/drawio`](../diagramas/drawio) y [`docs/diagramas/png`](../diagramas/png).
- [x] Evidencias de pruebas, cobertura JaCoCo, SonarQube, Swagger y despliegue almacenadas en [`docs/evidencias/`](../evidencias/).
- [x] Colección Postman y pruebas automatizadas de todos los endpoints disponibles en [`docs/postman/`](../postman/).
- [x] No se introducen credenciales ni secretos en el código (uso de variables de entorno).
- [x] El PR cuenta con la revisión y aprobación del líder técnico del squad.

---

[← Anterior: 10. CI/CD y Pipelines](./10-cicd-pipeline.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 12. Épica en Jira y Requerimientos →](./12-epica-jira-requerimientos.md)
