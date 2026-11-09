package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.dosw.styleradar.donaciones.application.dto.auth.LoginRequestDTO;
import edu.dosw.styleradar.donaciones.infrastructure.security.DemoUserService;
import edu.dosw.styleradar.donaciones.infrastructure.security.JwtAuthenticationFilter;
import edu.dosw.styleradar.donaciones.infrastructure.security.JwtTokenProvider;
import edu.dosw.styleradar.donaciones.infrastructure.security.Role;
import edu.dosw.styleradar.donaciones.infrastructure.security.SecurityConfig;
import edu.dosw.styleradar.donaciones.infrastructure.security.SecurityErrorHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtTokenProvider.class,
        DemoUserService.class,
        SecurityErrorHandler.class,
        GlobalExceptionHandler.class
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /auth/login - Debe autenticar exitosamente y retornar token Bearer para usuario normal")
    void shouldLoginNormalUser() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("user@styleradar.com", "User123*");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.userId").value(101))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("POST /auth/login - Debe rechazar credenciales incorrectas")
    void shouldRejectInvalidCredentials() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("user@styleradar.com", "clave-invalida");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /auth/quick-token - Debe retornar token inmediato para ADMIN")
    void shouldGetQuickTokenAdmin() throws Exception {
        mockMvc.perform(get("/auth/quick-token")
                        .param("role", Role.ADMIN.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("GET /auth/quick-token - Debe retornar token inmediato para FOUNDATION")
    void shouldGetQuickTokenFoundation() throws Exception {
        mockMvc.perform(get("/auth/quick-token")
                        .param("role", Role.FOUNDATION.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("FOUNDATION"))
                .andExpect(jsonPath("$.userId").value(501))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("GET /auth/demo-users - Debe listar los 3 usuarios demo del sistema")
    void shouldListDemoUsers() throws Exception {
        mockMvc.perform(get("/auth/demo-users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].email").value("admin@styleradar.com"))
                .andExpect(jsonPath("$[1].email").value("user@styleradar.com"))
                .andExpect(jsonPath("$[2].email").value("foundation@styleradar.com"));
    }
}
