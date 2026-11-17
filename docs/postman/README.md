# Colección Postman — `ms-segunda-vida-donaciones` (`/api/v1`)

Carpeta oficial de pruebas funcionales, seguridad RBAC/BOLA, reglas de dominio e integración para **todos los endpoints** del microservicio `PT8080-Donations-Service`.

## Archivos incluidos

| Archivo | Propósito |
| :--- | :--- |
| [`StyleRadar-Donations-Service.postman_collection.json`](StyleRadar-Donations-Service.postman_collection.json) | Colección Postman v2.1 con **6 carpetas**, **108 peticiones** y aserciones automáticas (`pm.test`) para cada código HTTP. |
| [`StyleRadar-Local.postman_environment.json`](StyleRadar-Local.postman_environment.json) | Variables de entorno para ejecución local (`http://localhost:8520` + mock `ms-tiendas` en `:8140`). |
| [`StyleRadar-QA.postman_environment.json`](StyleRadar-QA.postman_environment.json) | Variables de entorno para Azure QA (`https://styleradar-donaciones-qa-jmunar.azurewebsites.net`). |
| [`mock-tiendas.js`](mock-tiendas.js) | Simulador ligero en Node.js de `ms-tiendas` (`:8140`) para probar `200`, `400` (fundación inactiva `99`), `404` (`9999`) y `503` (`503` / caída temporal) sin depender de otro repositorio. |
| [`test-garment.png`](test-garment.png) / [`invalid-photo.txt`](invalid-photo.txt) | Archivos binarios de prueba para validar subida exitosa (`201`) y rechazo de formato inválido (`400`) en `/api/v1/second-hand/photos`. |

## Cómo ejecutar en Postman (Interfaz Gráfica)

1. Abrir **Postman** → **Import** y seleccionar `StyleRadar-Donations-Service.postman_collection.json` y `StyleRadar-Local.postman_environment.json`.
2. Seleccionar el entorno **StyleRadar Donations - Local (8520)** en la esquina superior derecha.
3. En Postman **Settings → General**, definir el **Working Directory** en la raíz del repositorio (`PT8080-Donations-Service`) para que las rutas `docs/postman/test-garment.png` e `invalid-photo.txt` resuelvan automáticamente.
4. Iniciar el microservicio (`mvn spring-boot:run`) y, si `ms-tiendas` no está levantado localmente, iniciar `node docs/postman/mock-tiendas.js`.
5. Hacer clic en **Run collection** sobre `StyleRadar - ms-segunda-vida-donaciones` y ejecutar en orden de arriba hacia abajo (la colección guarda tokens e IDs dinámicos en `collectionVariables`).

## Cómo ejecutar por CLI con Newman

```bash
# Terminal 1: Mock ms-tiendas (si ms-tiendas real no está corriendo en :8140)
node docs/postman/mock-tiendas.js

# Terminal 2: Microservicio local (perfil local con H2)
mvn spring-boot:run

# Terminal 3: Ejecutar toda la suite con Newman desde la raíz del repositorio
npx newman run docs/postman/StyleRadar-Donations-Service.postman_collection.json -e docs/postman/StyleRadar-Local.postman_environment.json
```

> **Nota sobre `HTTP 500`:** Los códigos `500 Internal Server Error` documentados en OpenAPI corresponden a fallos imprevistos de servidor/infraestructura (`GlobalExceptionHandler.handleGeneralException`) y no deben provocarse en un servicio sano. El resto de códigos funcionales, de validación, seguridad e integración externa (`200`, `201`, `204`, `400`, `401`, `403`, `404`, `409`, `503`) están cubiertos y verificados automáticamente.

## Cobertura de Endpoints y Códigos HTTP

| Endpoint | Códigos cubiertos |
| :--- | :--- |
| `GET /api/v1/health` | `200` |
| `GET /api/v1/auth/demo-users` | `200` |
| `POST /api/v1/auth/login` | `200`, `400` |
| `GET /api/v1/auth/quick-token` | `200`, `400` |
| `POST /api/v1/second-hand/photos` | `201`, `400`, `401` |
| `POST /api/v1/second-hand/publications` | `201`, `400`, `401`, `403` |
| `GET /api/v1/second-hand/publications` | `200`, `400` |
| `GET /api/v1/second-hand/publications/{id}` | `200`, `404` |
| `GET /api/v1/second-hand/publications/users/{userId}` | `200`, `401`, `403` |
| `PUT /api/v1/second-hand/publications/{id}` | `200`, `400`, `401`, `403`, `404`, `409` |
| `PATCH /api/v1/second-hand/publications/{id}/sale` | `200`, `400`, `401`, `403`, `404`, `409` |
| `DELETE /api/v1/second-hand/publications/{id}` | `204`, `401`, `403`, `404`, `409` |
| `PATCH /api/v1/second-hand/publications/{id}/donation` | `200`, `401`, `403`, `404`, `409` |
| `GET /api/v1/donations/foundations` | `200`, `401`, `503` |
| `GET /api/v1/donations/foundations/representative` | `200`, `401`, `403`, `404`, `503` |
| `GET /api/v1/donations/foundations/{foundationId}` | `200`, `400`, `401`, `404`, `503` |
| `POST /api/v1/donations/claims` | `201`, `400`, `401`, `403`, `404`, `409`, `503` |
| `POST /api/v1/donations` | `201`, `400`, `401`, `403`, `404`, `409`, `503` |
| `GET /api/v1/donations/{id}` | `200`, `401`, `403`, `404` |
| `GET /api/v1/donations/donors/{donorId}` | `200`, `401`, `403` |
| `GET /api/v1/donations/foundations/{foundationId}/requests` | `200`, `401`, `403` |
| `PATCH /api/v1/donations/{id}/decision` | `200` (aprobar y rechazar), `400`, `401`, `403`, `404`, `409` |
| `PATCH /api/v1/donations/{id}/pickup` | `200`, `400`, `401`, `403`, `404`, `409` |
| `PATCH /api/v1/donations/{id}/confirmation` | `200`, `400`, `401`, `403`, `404`, `409` |
