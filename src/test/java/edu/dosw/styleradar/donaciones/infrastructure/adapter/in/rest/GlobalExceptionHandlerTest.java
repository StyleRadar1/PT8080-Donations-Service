package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;

import edu.dosw.styleradar.donaciones.domain.exception.DomainConflictException;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidPublicationException;
import edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Debe manejar AccessDeniedException con status 403 y RFC 7807")
    void shouldHandleAccessDenied() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/segunda-mano");
        AccessDeniedException ex = new AccessDeniedException("Acceso denegado");

        ProblemDetail problem = handler.handleAccessDenied(ex, request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(problem.getTitle()).isEqualTo("Acceso denegado");
        assertThat(problem.getDetail()).isEqualTo("Acceso denegado");
        assertThat(problem.getInstance().getPath()).isEqualTo("/api/v1/segunda-mano");
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("Debe manejar ResourceNotFoundException con status 404 y RFC 7807")
    void shouldHandleNotFound() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/segunda-mano/99");
        ResourceNotFoundException ex = new ResourceNotFoundException("No encontrado");

        ProblemDetail problem = handler.handleNotFound(ex, request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Recurso no encontrado");
        assertThat(problem.getDetail()).isEqualTo("No encontrado");
        assertThat(problem.getInstance().getPath()).isEqualTo("/api/v1/segunda-mano/99");
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("Debe manejar excepciones de solicitud inválida con status 400 y RFC 7807")
    void shouldHandleBadRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/segunda-mano");
        InvalidPublicationException ex = new InvalidPublicationException("Invariante violada");

        ProblemDetail problem = handler.handleBadRequest(ex, request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Solicitud inválida");
        assertThat(problem.getDetail()).isEqualTo("Invariante violada");
    }

    @Test
    @DisplayName("Debe manejar excepciones de conflicto de dominio con status 409 y RFC 7807")
    void shouldHandleConflict() {
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/v1/segunda-mano/1/sell");
        DomainConflictException ex = new DomainConflictException("Conflicto");

        ProblemDetail problem = handler.handleConflict(ex, request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("Conflicto de dominio");
        assertThat(problem.getDetail()).isEqualTo("Conflicto");
    }

    @Test
    @DisplayName("Debe manejar errores no controlados con status 500 y RFC 7807")
    void shouldHandleGeneralException() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/segunda-mano/1");
        Exception ex = new RuntimeException("Error inesperado");

        ProblemDetail problem = handler.handleGeneralException(ex, request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo("Error interno del servidor");
    }
}
