[← Anterior: 10. CI/CD y Pipelines](./09-cicd-pipeline.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 11. Seguridad y OWASP Top 10 →](./11-seguridad-y-owasp.md)

# 10. Ramas y Cómo Contribuir

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
- [x] Todas las pruebas automatizadas resultan aprobadas (`mvn -B clean verify` con **339 pruebas aprobadas**, `0` fallos, `0` errores, `0` omitidas con Docker/Testcontainers PostgreSQL; último `develop` `4a2cbec` aprobado y desplegado en QA: [Run CI/CD QA `develop` (#45728635298)](https://github.com/StyleRadar1/PT8080-Donations-Service/actions/runs/45728635298)).
- [x] La cobertura en JaCoCo supera el `85.00%` en todas las dimensiones clave (**97.50% líneas / 97.52% instrucciones / 88.24% ramas / 95.83% métodos**, detallado en [`09-pruebas-cobertura.md`](./08-pruebas-y-cobertura.md)).
- [x] Diagramas de arquitectura actualizados en [`docs/diagramas/drawio`](../diagramas/drawio) y [`docs/diagramas/png`](../diagramas/png).
- [x] Evidencias de pruebas, cobertura JaCoCo, SonarQube, Swagger y despliegue almacenadas en [`docs/evidencias/`](../evidencias/).
- [x] Colección Postman y pruebas automatizadas de todos los endpoints disponibles en [`docs/postman/`](../postman/).
- [x] No se introducen credenciales ni secretos en el código (uso de variables de entorno).
- [x] El PR cuenta con la revisión y aprobación del líder técnico del squad.

---

[← Anterior: 10. CI/CD y Pipelines](./09-cicd-pipeline.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 11. Seguridad y OWASP Top 10 →](./11-seguridad-y-owasp.md)
