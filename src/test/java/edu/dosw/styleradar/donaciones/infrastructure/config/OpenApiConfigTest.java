package edu.dosw.styleradar.donaciones.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
