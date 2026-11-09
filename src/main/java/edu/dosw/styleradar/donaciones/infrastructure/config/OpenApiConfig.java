package edu.dosw.styleradar.donaciones.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración de OpenAPI 3 / Swagger UI con soporte para autenticación Bearer JWT y esquemas de seguridad.
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
                        new Server().url(contextPath).description("Servidor Local - Context Path")
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
                        new Tag().name("Autenticación & Usuarios Demo").description("Inicio de sesión y generación rápida de tokens JWT para los 3 roles del sistema"),
                        new Tag().name("Publicaciones de Segunda Mano").description("Gestión del ciclo de vida C2C de ropa de segunda mano (SR-RF-33 a SR-RF-39 y SR-RF-42)"),
                        new Tag().name("Solicitudes de Donación").description("Flujo altruista y logística de donaciones hacia fundaciones benéficas aliadas (SR-RF-44 a SR-RF-49)"),
                        new Tag().name("Health Check").description("Monitoreo de disponibilidad y estado operativo del microservicio")
                ))
                .info(new Info()
                        .title("STYLE RADAR · ms-segunda-vida-donaciones API")
                        .version("v0.0.1-SNAPSHOT")
                        .description("""
                                ### Microservicio de Donaciones y Economía Circular de StyleRadar (Paquete 5)
                                Gestiona el ciclo completo de publicaciones C2C de prendas de segunda mano y las solicitudes de donación dirigidas a fundaciones aliadas.
                                
                                #### Roles de Usuario del Sistema (RBAC):
                                - **ADMIN (ROLE_ADMIN)**: Administrador de plataforma con control global de moderación, retiro forzado de publicaciones y supervisión de todas las solicitudes de donación.
                                - **USER (ROLE_USER)**: Usuario normal de StyleRadar (comprador, vendedor y donante). Puede publicar sus prendas usadas, editarlas, marcarlas como vendidas, retirarlas, destinarlas a donación, crear solicitudes hacia fundaciones y programar la logística de recogida.
                                - **FOUNDATION (ROLE_FOUNDATION)**: Representante de fundación benéfica aliada. Puede consultar solicitudes dirigidas a su fundación, decidir sobre ellas (aprobar o rechazar) y confirmar la entrega física de las prendas donadas.
                                
                                #### Autenticación y Pruebas en Swagger:
                                Utilice la sección **Autenticación & Usuarios Demo** para obtener tokens de prueba válidos para cualquiera de los tres roles mediante un solo clic.
                                """)
                        .contact(new Contact()
                                .name("Squad StyleRadar - DOSW 2026")
                                .email("soporte@styleradar.escuelaing.edu.co"))
                        .license(new License().name("Apache 2.0")));
    }
}
