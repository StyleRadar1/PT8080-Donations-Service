package edu.dosw.styleradar.donaciones.infrastructure.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

/**
 * Configuración de OpenAPI 3 / Swagger UI con soporte para autenticación Bearer JWT,
 * esquemas de seguridad, y documentación exhaustiva del módulo de Economía Circular en Bogotá D.C.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Value("${server.servlet.context-path:/api/v1}")
    private String contextPath;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server().url(contextPath).description("Servidor Local / Staging - Context Path")
                ))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("""
                                        Autenticación Bearer JWT.
                                        Para autenticarse en Swagger:
                                        1. Obtenga un token llamando a `GET /auth/quick-token?role=ADMIN|USER|FOUNDATION` o `POST /auth/login`.
                                        2. Copie el token del campo `token`.
                                        3. Haga clic en el botón verde **Authorize** arriba a la derecha.
                                        4. Pegue el token en el campo `Value` y presione **Authorize**.
                                        """))
                )
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .tags(List.of(
                        new Tag().name("Autenticación & Usuarios Demo").description("Inicio de sesión y generación instantánea de tokens JWT para los roles del sistema"),
                        new Tag().name("Publicaciones - Economía Circular").description("Catálogo unificado C2C (Segunda Mano) y Vitrina Abierta (Donaciones Benéficas) en las 20 localidades de Bogotá D.C. (SR-RF-33 a SR-RF-42)"),
                        new Tag().name("Donaciones - Vitrina Benéfica y Logística").description("Reclamos de vitrina por fundaciones, aprobación y citas logísticas de recogida (SR-RF-44 a SR-RF-49)"),
                        new Tag().name("Health Check").description("Monitoreo de disponibilidad y estado operativo del microservicio")
                ))
                .info(new Info()
                        .title("STYLE RADAR · ms-segunda-vida-donaciones API")
                        .version("v0.0.1-SNAPSHOT")
                        .description("""
                                ### Microservicio de Donaciones y Economía Circular de StyleRadar (Paquete 5 · Puerto 8520)
                                Unifica en la misma base de datos relacional PostgreSQL dos flujos complementarios de economía circular para prendas únicas en **Bogotá D.C.**:
                                
                                1. **Segunda Mano C2C (`esDonacion = false`)**:
                                   - Prendas con precio asignado por el usuario (> 0 COP).
                                   - Botón *Chatear con el vendedor* para acordar entrega o precio mediante integración con `ms-chat`.
                                   - Al concretar la venta, el vendedor la marca como `VENDIDA`, emitiendo el evento RabbitMQ `styleradar.segundavida.prenda.vendida`.
                                
                                2. **Donaciones Benéficas (`esDonacion = true`)**:
                                   - Prendas con precio forzoso 0 COP para canal benéfico.
                                   - Publicadas en la **Vitrina Abierta** donde fundaciones aliadas pueden reclamarlas directamente.
                                   - Control de concurrencia optimista (`@Version`): si dos fundaciones reclaman al tiempo, solo una gana (409 Conflict).
                                   - Regla anti-auto-donación: una fundación no puede reclamar su propia prenda.
                                   - Tras el reclamo, se coordina la logística de recogida y entrega física con eventos `styleradar.donaciones.estado.cambiado`.
                                
                                3. **Restricción Territorial Bogotá D.C.**:
                                   - StyleRadar opera exclusivamente en las 20 localidades oficiales de Bogotá D.C.
                                
                                #### Roles de Usuario (RBAC):
                                - **ADMIN (ROLE_ADMIN)**: Acceso total para supervisión y moderación.
                                - **USER (ROLE_USER)**: Venta C2C de ropa usada y donación benéfica.
                                - **STORE (ROLE_STORE)**: Tiendas que destinan excedentes a donación benéfica.
                                - **FOUNDATION (ROLE_FOUNDATION)**: Fundaciones receptoras que reclaman prendas de la vitrina, deciden y confirman entregas.
                                """)
                        .contact(new Contact()
                                .name("Squad StyleRadar - DOSW 2026")
                                .email("soporte@styleradar.escuelaing.edu.co"))
                        .license(new License().name("Apache 2.0")));
    }
}
