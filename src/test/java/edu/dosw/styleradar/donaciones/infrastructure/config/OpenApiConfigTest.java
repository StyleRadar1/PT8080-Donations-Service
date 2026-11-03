package edu.dosw.styleradar.donaciones.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OpenApiConfigTest {

    @Test
    @DisplayName("Debe instanciar y configurar la especificación OpenAPI con context path")
    void shouldConfigureOpenAPI() {
        OpenApiConfig config = new OpenApiConfig();
        ReflectionTestUtils.setField(config, "contextPath", "/api/v1");

        OpenAPI openAPI = config.customOpenAPI();

        assertNotNull(openAPI);
        assertNotNull(openAPI.getInfo());
        assertEquals("STYLE RADAR · ms-segunda-vida-donaciones API", openAPI.getInfo().getTitle());
        assertEquals("v0.0.1-SNAPSHOT", openAPI.getInfo().getVersion());
        assertEquals(1, openAPI.getServers().size());
        assertEquals("/api/v1", openAPI.getServers().get(0).getUrl());
    }
}
