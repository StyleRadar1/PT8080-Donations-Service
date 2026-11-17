package edu.dosw.styleradar.donaciones.infrastructure.security;

import edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest.GlobalExceptionHandler;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest.HealthController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración HTTP que valida que en perfil "qa" los endpoints de tokens demo
 * (/api/v1/auth/**) NO quedan expuestos ni permitidos sin autenticación.
 */
@WebMvcTest(
        controllers = HealthController.class,
        properties = {
                "styleradar.security.jwt.secret-base64=c3R5bGVyYWRhci1zZWNyZXQta2V5LWZvci1kb3N3LTIwMjYtcHJvamVjdC1qd3Q=",
                "styleradar.security.publication-by-id-path=/api/v1/second-hand/publications/{id}"
        }
)
@ActiveProfiles("qa")
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtTokenProvider.class,
        SecurityErrorHandler.class,
        GlobalExceptionHandler.class
})
class AuthQaSecurityIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("En perfil qa - /api/v1/auth/quick-token y /demo-users deben quedar bloqueados (401)")
    void shouldBlockDemoAuthEndpointsInQaProfile() throws Exception {
        mockMvc.perform(get("/api/v1/auth/quick-token").param("role", "ADMIN"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/auth/demo-users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk());
    }
}
