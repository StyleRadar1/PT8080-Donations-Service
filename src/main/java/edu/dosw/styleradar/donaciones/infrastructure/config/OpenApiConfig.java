package edu.dosw.styleradar.donaciones.infrastructure.config;

import java.util.List;

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

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server().url("/").description("StyleRadar Server (Local / QA / PROD)")
                ))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("""
                                        Bearer JWT Authentication.
                                        To authenticate in Swagger:
                                        1. Obtain a token by calling `GET /api/v1/auth/quick-token?role=ADMIN|USER|FOUNDATION` or `POST /api/v1/auth/login`.
                                        2. Copy the value of the `token` field.
                                        3. Click the green **Authorize** button at the top right.
                                        4. Paste the token into the `Value` field and click **Authorize**.
                                        """))
                )
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .tags(List.of(
                        new Tag().name("Demo Authentication & Users").description("Login endpoints and instant JWT token generation for testing system roles"),
                        new Tag().name("Second-Hand & Circular Economy Publications").description("Unified C2C Second-Hand catalog and Open Donation Showcase across the 20 districts of Bogota D.C."),
                        new Tag().name("Garment Photo Upload (Azure Blob Storage)").description("Binary image upload to Azure Blob Storage returning public CDN URLs for garment listings"),
                        new Tag().name("Charitable Donations & Logistics").description("Open showcase claims by partner foundations, request evaluation, pickup scheduling, and delivery confirmation"),
                        new Tag().name("Health Check").description("Service availability and operational health monitoring")
                ))
                .info(new Info()
                        .title("STYLE RADAR · ms-segunda-vida-donaciones API")
                        .version("v0.0.1-SNAPSHOT")
                        .description("""
                                ### StyleRadar Donations & Circular Economy Microservice (Port 8520)
                                Unifies two complementary circular textile economy workflows for unique garments in **Bogota D.C.** using a dedicated PostgreSQL database:
                                
                                1. **C2C Second-Hand Resale (`esDonacion = false`)**:
                                   - Garments priced strictly above 0 COP by the owner.
                                   - Direct buyer-seller coordination through `ms-chat`.
                                   - Marking a garment as `SOLD` emits a RabbitMQ integration event (`segunda-mano.prenda.vendida`).
                                
                                2. **Charitable Donations (`esDonacion = true`)**:
                                   - Garments fixed at 0 COP for partner charities.
                                   - Exposed in the **Open Donation Showcase** where verified partner foundations can claim items directly.
                                   - Optimistic locking (`@Version`): concurrent foundation claims resolve atomically (409 Conflict for losers).
                                   - Anti-self-donation rule: a foundation cannot claim its own listing.
                                   - Status transitions publish integration events (`styleradar.donaciones.solicitud.creada` and `styleradar.donaciones.estado.cambiado`).
                                
                                3. **Territorial Restriction (Bogota D.C.)**:
                                   - StyleRadar operates exclusively across the 20 official districts (localidades) of Bogota D.C.
                                
                                #### User Roles (RBAC):
                                - **ADMIN (ROLE_ADMIN)**: Full administrative oversight and moderation access.
                                - **USER (ROLE_USER)**: C2C garment resale and charitable donation requests.
                                - **STORE (ROLE_STORE)**: Partner stores donating surplus inventory.
                                - **FOUNDATION (ROLE_FOUNDATION)**: Verified charities claiming garments, evaluating requests, and confirming physical delivery.
                                """)
                        .contact(new Contact()
                                .name("Squad StyleRadar - DOSW 2026")
                                .email("soporte@styleradar.escuelaing.edu.co"))
                        .license(new License().name("Apache 2.0")));
    }
}
