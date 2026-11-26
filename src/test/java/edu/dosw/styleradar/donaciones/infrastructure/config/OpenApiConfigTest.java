package edu.dosw.styleradar.donaciones.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.tags.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OpenApiCustomizer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OpenApiConfigTest {

    @Test
    @DisplayName("Debe instanciar y configurar la especificación OpenAPI con servidor raíz para exponer rutas /api/v1")
    void shouldConfigureOpenAPI() {
        OpenApiConfig config = new OpenApiConfig();

        OpenAPI openAPI = config.customOpenAPI();

        assertNotNull(openAPI);
        assertNotNull(openAPI.getInfo());
        assertEquals("STYLE RADAR · ms-segunda-vida-donaciones API", openAPI.getInfo().getTitle());
        assertEquals("v0.0.1-SNAPSHOT", openAPI.getInfo().getVersion());
        assertEquals(1, openAPI.getServers().size());
        assertEquals("/", openAPI.getServers().get(0).getUrl());
        assertNotNull(openAPI.getComponents().getSecuritySchemes().get("bearerAuth"));
        assertEquals("bearer", openAPI.getComponents().getSecuritySchemes().get("bearerAuth").getScheme());
        assertEquals("JWT", openAPI.getComponents().getSecuritySchemes().get("bearerAuth").getBearerFormat());
        assertEquals(1, openAPI.getSecurity().size());
        assertEquals(true, openAPI.getSecurity().get(0).containsKey("bearerAuth"));
        assertEquals(5, openAPI.getTags().size());
    }

    @Test
    @DisplayName("Debe eliminar tags sin endpoints activos (como Demo Auth en QA) y evitar etiquetas duplicadas")
    void shouldRemoveEmptyAndDuplicateTagsInSwagger() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openAPI = config.customOpenAPI();
        openAPI.setTags(List.of(
                new Tag().name("Demo Authentication & Users").description("Empty in QA"),
                new Tag().name("Health Check").description("Salud 1"),
                new Tag().name("Health Check").description("Salud duplicado"),
                new Tag().name("Charitable Donations & Logistics").description("Donaciones")
        ));
        openAPI.setPaths(new Paths()
                .addPathItem("/api/v1/health", new PathItem().get(new Operation().addTagsItem("Health Check")))
                .addPathItem("/api/v1/donations", new PathItem().get(new Operation().addTagsItem("Charitable Donations & Logistics"))));

        OpenApiCustomizer customizer = config.removeEmptyAndDuplicateTagsCustomizer();
        customizer.customise(openAPI);

        assertEquals(2, openAPI.getTags().size());
        assertEquals(List.of("Health Check", "Charitable Donations & Logistics"),
                openAPI.getTags().stream().map(Tag::getName).toList());
    }
}
