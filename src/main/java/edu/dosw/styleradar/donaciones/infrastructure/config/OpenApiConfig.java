package edu.dosw.styleradar.donaciones.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.servlet.context-path:/api/v1}")
    private String contextPath;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server().url(contextPath).description("Servidor Local - Context Path")
                ))
                .info(new Info()
                        .title("STYLE RADAR · ms-segunda-vida-donaciones API")
                        .version("v0.0.1-SNAPSHOT")
                        .description("Microservicio de Donaciones y Economía Circular de StyleRadar (Paquete 5). Gestiona publicaciones C2C de segunda mano y solicitudes de donaciones a fundaciones aliadas.")
                        .contact(new Contact()
                                .name("Squad StyleRadar - DOSW 2026")
                                .email("soporte@styleradar.escuelaing.edu.co"))
                        .license(new License().name("Apache 2.0")));
    }
}
