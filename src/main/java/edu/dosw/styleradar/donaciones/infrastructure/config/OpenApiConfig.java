package edu.dosw.styleradar.donaciones.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("StyleRadar - ms-segunda-vida-donaciones API")
                        .version("v0.0.1-SNAPSHOT")
                        .description("Microservicio de Economía Circular y Donaciones (Paquete 5).")
                        .contact(new Contact()
                                .name("Equipo PT8080 - DOSW")
                                .email("dev@styleradar.escuelaing.edu.co"))
                        .license(new License().name("Apache 2.0")));
    }
}
