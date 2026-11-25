# Colección Postman — `ms-segunda-vida-donaciones` (`/api/v1`)

Carpeta oficial de pruebas funcionales, seguridad RBAC/BOLA, reglas de dominio e integración para **todos los endpoints** del microservicio `PT8080-Donations-Service`.

## Archivos incluidos

| Archivo | Propósito |
| :--- | :--- |
| [`StyleRadar-Donations-Service.postman_collection.json`](StyleRadar-Donations-Service.postman_collection.json) | Colección Postman v2.1 con **6 carpetas**, **110 peticiones** y aserciones automáticas (`pm.test`) para cada código HTTP. |
| [`StyleRadar-Local.postman_environment.json`](StyleRadar-Local.postman_environment.json) | Variables de entorno para ejecución local (`http://localhost:8520` + mock `ms-tiendas` en `:8140`). |
| [`StyleRadar-QA.postman_environment.json`](StyleRadar-QA.postman_environment.json) | Variables de entorno para Azure QA (`https://styleradar-donaciones-qa-jmunar.azurewebsites.net`). |
| [`mock-tiendas.js`](mock-tiendas.js) | Simulador ligero en Node.js de `ms-tiendas` (`:8140`) para probar `200`, `400` (fundación inactiva `99`), `404` (`9999`) y `503` (`503` / caída temporal) sin depender de otro repositorio. |
| [`test-garment.png`](test-garment.png) / [`invalid-photo.txt`](invalid-photo.txt) | Archivos binarios de prueba para validar subida exitosa (`201`) y rechazo de formato inválido (`400`) en `/api/v1/second-hand/photos`. |

## Cómo ejecutar en la App de Postman (Paso a paso)

### 1. Prerrequisitos en tu máquina (Entorno Local)
1. **RabbitMQ** activo en el puerto `5672` *(por ejemplo el contenedor `sr-rabbitmq-donaciones` de `docker-compose.yml`)*.
2. **Terminal 1 — Simulador de `ms-tiendas` (puerto `8140`):**
   ```powershell
   node docs/postman/mock-tiendas.js
   ```
   *(Permite probar los casos reales `200`, `400` con fundación inactiva `99`, `404` con `9999` y `503` sin necesitar otro microservicio encendido).*
3. **Terminal 2 — Microservicio en el puerto `8520`:**
   ```powershell
   mvn spring-boot:run
   ```
   *(Asegúrate de que el puerto `8520` no esté ocupado por otro contenedor Docker antes de arrancarlo).*

### 2. Importar Colección y Entorno en Postman
1. Abre la aplicación de **Postman** y haz clic en **Import** *(arriba a la izquierda)*.
2. Arrastra o selecciona estos dos archivos desde `docs/postman/`:
   * `StyleRadar-Donations-Service.postman_collection.json`
   * `StyleRadar-Local.postman_environment.json` *(opcionalmente `StyleRadar-QA.postman_environment.json` si probarás QA)*
3. En la esquina superior derecha de Postman, cambia el selector de entorno a **`StyleRadar Donations - Local (8520)`**.

### 3. Configurar el Working Directory (obligatorio para subir fotos)
Para que las 3 pruebas de la carpeta **`03 - Carga de Fotografías`** encuentren `test-garment.png` e `invalid-photo.txt` automáticamente:
1. En Postman abre **Settings** *(ícono de engranaje arriba a la derecha → **Settings** → **General**)*.
2. En **Working Directory** (o *Location*), selecciona o pega la ruta raíz del proyecto:
   `C:\Users\JuanM\Desktop\Code\DOSW\Final_Project\PT8080-Donations-Service`
3. *(Alternativa manual si no cambias el Working Directory: entra al tab **Body → form-data** de las 3 peticiones de la carpeta `03` y vuelve a seleccionar `docs/postman/test-garment.png` e `invalid-photo.txt`)*.

### 4. Ejecutar las pruebas
* **Opción A — Correr toda la suite automática (Collection Runner, recomendado):**
  1. En el panel izquierdo, haz clic en los **tres puntos (`...`)** junto a **`StyleRadar - ms-segunda-vida-donaciones (API v1 Complete Suite)`** → **Run**.
  2. Verifica que todas las carpetas (`01` a `06`) estén seleccionadas en su orden original.
  3. Haz clic en **Run StyleRadar - ms-segunda-vida-donaciones...**.
  4. Todas las peticiones guardan sus propios tokens (`tokenUser`, `tokenStore`, `tokenFoundation`, `tokenAdmin`) e IDs (`salePubId`, `retirePubId`, `donatePubId`, `rejectDonatePubId`, `openShowcasePubId`, `donationRequestId`, `rejectRequestId`, `claimedRequestId`), por lo que debe cerrar con **110/110 assertions passed** (y puedes volver a darle *Run again* las veces que quieras).
* **Opción B — Probar peticiones una por una:**
  1. Ejecuta primero las peticiones `200` de **`02 - Autenticación Demo`** (para generar y guardar los 4 tokens JWT) y la `201` de **`03 - Carga de Fotografías`** (para guardar `uploadedPhotoUrl`).
  2. En las carpetas **`04`**, **`05`** y **`06`**, respeta el orden de aparición dentro de cada endpoint, porque las pruebas `409 Conflict` reutilizan las publicaciones/solicitudes recién vendidas, reclamadas o confirmadas en las peticiones anteriores.

> **Si pruebas contra Azure QA (`StyleRadar Donations - Azure QA`):** cambia el entorno arriba a la derecha a `StyleRadar Donations - Azure QA`. Ten presente que en QA no corre `mock-tiendas.js`, por lo que los casos con IDs ficticios (`3`, `99`, `503`, `9999`) dependerán de las fundaciones reales registradas en `ms-tiendas` QA.

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
| `PATCH /api/v1/second-hand/publications/{id}/sale?userId={userId}&compradorId={compradorId}` | `200`, `400`, `401`, `403`, `404`, `409` |
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
