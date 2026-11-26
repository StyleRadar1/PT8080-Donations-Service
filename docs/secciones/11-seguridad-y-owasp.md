[⬅ Volver al README Principal](../../README.md)

---

## 11. SEGURIDAD, CONTROL DE ACCESO RBAC Y ANÁLISIS OWASP TOP 10:2025

El microservicio `PT8080-Donations-Service` (`ms-segunda-vida-donaciones`) implementa las directrices oficiales de seguridad de StyleRadar para autenticación Stateless JWT, control de titularidad (anti-BOLA/IDOR), integridad transaccional y mitigación de vulnerabilidades **OWASP Top 10:2025**.

* **Documento Técnico Oficial:** [`../security/SR_PT8080_Seguridad_y_OWASP_2025.md`](../security/SR_PT8080_Seguridad_y_OWASP_2025.md)

### Puntos de Control y Cumplimiento Implementados

1. **Autenticación Bearer JWT y Perfiles Endurecidos (`A02`, `A07`):**
   * Validación criptográfica `HS256` (`>= 256 bits`) en `JwtTokenProvider` y `JwtAuthenticationFilter` con política `SessionCreationPolicy.STATELESS`.
   * Soporte para claims oficiales de `ms-auth` (`sub`, `roles`, `role`) y bloqueo automático del arranque en `qa`/`prod` si la clave secreta JWT está ausente o es insegura.
   * Los endpoints auxiliares de demostración (`/api/v1/auth/**`) permanecen deshabilitados en `qa` y `prod`.

2. **Autorización RBAC y Prevención de IDOR / BOLA (`A01`):**
   * Control granular en `SecurityConfig` (`@EnableMethodSecurity`) diferenciando los roles `USER`, `STORE`, `FOUNDATION` y `ADMIN`.
   * Validación estricta de titularidad en `ResourceOwnershipValidator` para publicaciones, solicitudes, bandejas de donantes y bandejas de fundaciones.
   * En `POST /api/v1/donations`, el identificador `donorId` se obtiene exclusivamente desde el token JWT; en `POST /api/v1/donations/claims`, se prohíbe al propietario reclamar sus propias prendas y `TiendasFoundationCatalogAdapter` verifica el `representativeId` mediante canales internos autenticados.

3. **Integridad Transaccional y Contratos Homologados (`A04`, `A05`, `A08`):**
   * Bloqueo optimista (`@Version`) ante reclamos concurrentes en vitrina abierta (`409 Conflict`).
   * Patrón **Transactional Outbox** (`eventos_outbox_donacion`) conectado sobre `AMQPS :5671` con `publisher-confirm-type: correlated` y manejo de mensajes inválidos hacia `styleradar.donaciones.dlq`.
   * Manejo centralizado de errores (`GlobalExceptionHandler`, `SecurityErrorHandler`) bajo **RFC 7807** y fichas **CADche v8** (`type: https://api.styleradar.com/errors/{code}`), sanitizando mensajes técnicos internos.

4. **Trazabilidad Distribuida (`A09`):**
   * `CorrelationIdFilter` (`@Order(Ordered.HIGHEST_PRECEDENCE)`) captura o genera `X-Correlation-Id` y `X-Request-Id`, los registra en `MDC`, los retorna en la respuesta HTTP y `TiendasClientConfig` los propaga hacia `ms-tiendas`.

---

[← Anterior: 10. Ramas y Cómo Contribuir](./10-ramas-y-como-contribuir.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 12. Equipo, Responsables y Épica Jira →](./12-equipo-responsables-y-epica-jira.md)
