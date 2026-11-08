package edu.dosw.styleradar.donaciones.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityErrorHandlerTest {

    private SecurityErrorHandler securityErrorHandler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        securityErrorHandler = new SecurityErrorHandler(objectMapper);
    }

    @Test
    @DisplayName("commence() debe emitir respuesta 401 con RFC 7807 problem details")
    void shouldHandleCommence401() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/donaciones");
        MockHttpServletResponse response = new MockHttpServletResponse();

        securityErrorHandler.commence(request, response, new BadCredentialsException("Token inválido"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/problem+json;charset=UTF-8");
        assertThat(response.getContentAsString()).contains("No autorizado");
    }

    @Test
    @DisplayName("handle() debe emitir respuesta 403 con RFC 7807 problem details")
    void shouldHandleAccessDenied403() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/v1/donaciones/1/decide");
        MockHttpServletResponse response = new MockHttpServletResponse();

        securityErrorHandler.handle(request, response, new AccessDeniedException("Rol insuficiente"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/problem+json;charset=UTF-8");
        assertThat(response.getContentAsString()).contains("Acceso denegado");
    }
}
