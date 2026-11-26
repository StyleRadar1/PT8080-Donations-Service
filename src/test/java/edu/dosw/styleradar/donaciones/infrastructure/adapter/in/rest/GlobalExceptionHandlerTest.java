package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import edu.dosw.styleradar.donaciones.domain.exception.DomainConflictException;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidPublicationException;
import edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest.filter.CorrelationIdFilter;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void cleanupMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("Debe manejar AccessDeniedException con status 403 y RFC 7807")
    void shouldHandleAccessDenied() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/second-hand/publications");
        AccessDeniedException ex = new AccessDeniedException("Acceso denegado");

        ProblemDetail problem = handler.handleAccessDenied(ex, request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(problem.getType()).hasToString("https://api.styleradar.com/errors/forbidden");
        assertThat(problem.getTitle()).isEqualTo("Acceso denegado");
        assertThat(problem.getDetail()).isEqualTo("Acceso denegado");
        assertThat(problem.getInstance().getPath()).isEqualTo("/api/v1/second-hand/publications");
        assertThat(problem.getProperties()).containsEntry("code", "FORBIDDEN");
        assertThat(problem.getProperties()).containsKey("errors");
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("Debe manejar ResourceNotFoundException con status 404 y RFC 7807")
    void shouldHandleNotFound() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/second-hand/publications/99");
        ResourceNotFoundException ex = new ResourceNotFoundException("No encontrado");

        ProblemDetail problem = handler.handleNotFound(ex, request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getType()).hasToString("https://api.styleradar.com/errors/resource-not-found");
        assertThat(problem.getTitle()).isEqualTo("Recurso no encontrado");
        assertThat(problem.getDetail()).isEqualTo("No encontrado");
        assertThat(problem.getInstance().getPath()).isEqualTo("/api/v1/second-hand/publications/99");
        assertThat(problem.getProperties()).containsEntry("code", "RESOURCE_NOT_FOUND");
        assertThat(problem.getProperties()).containsKey("errors");
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("Debe manejar excepciones de solicitud inválida con status 400 y RFC 7807")
    void shouldHandleBadRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/second-hand/publications");
        InvalidPublicationException ex = new InvalidPublicationException("Invariante violada");

        ProblemDetail problem = handler.handleBadRequest(ex, request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getType()).hasToString("https://api.styleradar.com/errors/bad-request");
        assertThat(problem.getTitle()).isEqualTo("Solicitud inválida");
        assertThat(problem.getDetail()).isEqualTo("Invariante violada");
        assertThat(problem.getProperties()).containsEntry("code", "BAD_REQUEST");
        assertThat(problem.getProperties()).containsKey("errors");
    }

    @Test
    @DisplayName("Debe manejar excepciones de conflicto de dominio con status 409 y RFC 7807")
    void shouldHandleConflict() {
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/v1/second-hand/publications/1/sale");
        DomainConflictException ex = new DomainConflictException("Conflicto");

        ProblemDetail problem = handler.handleConflict(ex, request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getType()).hasToString("https://api.styleradar.com/errors/domain-conflict");
        assertThat(problem.getTitle()).isEqualTo("Conflicto de dominio");
        assertThat(problem.getDetail()).isEqualTo("Conflicto");
        assertThat(problem.getProperties()).containsEntry("code", "DOMAIN_CONFLICT");
        assertThat(problem.getProperties()).containsKey("errors");
    }

    @Test
    @DisplayName("Debe manejar errores no controlados con status 500 y RFC 7807")
    void shouldHandleGeneralException() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/second-hand/publications/1");
        Exception ex = new RuntimeException("Error inesperado");

        ProblemDetail problem = handler.handleGeneralException(ex, request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getType()).hasToString("https://api.styleradar.com/errors/internal-server-error");
        assertThat(problem.getTitle()).isEqualTo("Error interno del servidor");
        assertThat(problem.getProperties()).containsEntry("code", "INTERNAL_SERVER_ERROR");
        assertThat(problem.getProperties()).containsKey("errors");
    }

    @Test
    @DisplayName("BUG-021 / BUG-031: Maneja type mismatch, 404, 405, 415, JSON inválido y parte faltante sin filtrar FQCN Java e incluye correlationId")
    void shouldHandleTypeMismatchHttpErrorsAndCorrelationIdWithoutLeakingFqcn() {
        MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, "corr-don-global-9");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/donations/invalid-id");

        MethodArgumentTypeMismatchException mismatchEx = new MethodArgumentTypeMismatchException(
                "invalid-id", Long.class, "id", null,
                new NumberFormatException("For input string: \"invalid-id\" java.lang.Long")
        );
        ProblemDetail mismatchPd = handler.handleTypeMismatch(mismatchEx, request);
        assertThat(mismatchPd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(mismatchPd.getDetail()).isEqualTo("El parámetro 'id' tiene un formato inválido.");
        assertThat(mismatchPd.getDetail()).doesNotContain("java.lang.Long").doesNotContain("java.lang.String");
        assertThat(mismatchPd.getProperties()).containsEntry("correlationId", "corr-don-global-9");

        ProblemDetail notFoundPd = handler.handleNoResourceFound(
                new NoResourceFoundException(HttpMethod.GET, "/api/v1/donations/unknown"), request
        );
        assertThat(notFoundPd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(notFoundPd.getDetail()).isEqualTo("El recurso solicitado no existe.");

        ProblemDetail methodPd = handler.handleMethodNotSupported(
                new HttpRequestMethodNotSupportedException("DELETE"), request
        );
        assertThat(methodPd.getStatus()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED.value());
        assertThat(methodPd.getProperties()).containsEntry("code", "METHOD_NOT_ALLOWED");

        ProblemDetail mediaPd = handler.handleMediaTypeNotSupported(
                new HttpMediaTypeNotSupportedException(MediaType.TEXT_PLAIN, java.util.List.of(MediaType.APPLICATION_JSON)), request
        );
        assertThat(mediaPd.getStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
        assertThat(mediaPd.getProperties()).containsEntry("code", "UNSUPPORTED_MEDIA_TYPE");

        ProblemDetail notReadablePd = handler.handleNotReadable(
                new HttpMessageNotReadableException("JSON parse error: java.lang.String", (HttpInputMessage) null), request
        );
        assertThat(notReadablePd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(notReadablePd.getType()).hasToString("https://api.styleradar.com/errors/malformed-request-body");
        assertThat(notReadablePd.getDetail()).doesNotContain("java.lang.String");

        ProblemDetail missingPartPd = handler.handleMissingPart(
                new MissingServletRequestPartException("file"), request
        );
        assertThat(missingPartPd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(missingPartPd.getType()).hasToString("https://api.styleradar.com/errors/missing-request-part");
        assertThat(missingPartPd.getDetail()).isEqualTo("Falta la parte requerida 'file' en la solicitud.");

        ProblemDetail sanitizedEnumPd = handler.handleBadRequest(
                new IllegalArgumentException("No enum constant edu.dosw.styleradar.donaciones.domain.model.GarmentType.INVALID"),
                request
        );
        assertThat(sanitizedEnumPd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(sanitizedEnumPd.getDetail()).doesNotContain("edu.dosw.");
    }

    @Test
    @DisplayName("Debe cubrir sanitización de mensajes nulos/vacíos/técnicos y resolución de correlationId desde cabecera HTTP")
    void shouldCoverSanitizationAndHeaderCorrelationFallbackBranches() {
        MockHttpServletRequest reqWithHeader = new MockHttpServletRequest("GET", "/api/v1/donations");
        reqWithHeader.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "  corr-header-77  ");

        MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, "   ");
        ProblemDetail nullMsgPd = handler.handleBadRequest(new IllegalArgumentException((String) null), reqWithHeader);
        assertThat(nullMsgPd.getDetail()).isEqualTo("La solicitud contiene parámetros inválidos.");
        assertThat(nullMsgPd.getProperties()).containsEntry("correlationId", "corr-header-77");

        MDC.clear();
        MockHttpServletRequest reqBlankHeader = new MockHttpServletRequest("GET", "/api/v1/donations");
        reqBlankHeader.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "   ");
        ProblemDetail blankMsgPd = handler.handleBadRequest(new IllegalArgumentException("   "), reqBlankHeader);
        assertThat(blankMsgPd.getDetail()).isEqualTo("La solicitud contiene parámetros inválidos.");
        assertThat(blankMsgPd.getProperties()).doesNotContainKey("correlationId");

        ProblemDetail javaMsgPd = handler.handleBadRequest(new IllegalArgumentException("Failed for java.lang.Long"), null);
        assertThat(javaMsgPd.getDetail()).isEqualTo("Uno de los parámetros enviados tiene un formato inválido.");
        assertThat(javaMsgPd.getInstance()).isNull();

        ProblemDetail enumOnlyPd = handler.handleBadRequest(new IllegalArgumentException("No enum constant X"), reqBlankHeader);
        assertThat(enumOnlyPd.getDetail()).isEqualTo("Uno de los parámetros enviados tiene un formato inválido.");

        MethodArgumentTypeMismatchException blankNameEx = new MethodArgumentTypeMismatchException(
                "bad", Long.class, "   ", null, new NumberFormatException("bad"));
        ProblemDetail blankParamPd = handler.handleTypeMismatch(blankNameEx, reqBlankHeader);
        assertThat(blankParamPd.getDetail()).isEqualTo("El parámetro 'parámetro' tiene un formato inválido.");

        ProblemDetail blankMethodPd = handler.handleMethodNotSupported(new HttpRequestMethodNotSupportedException(""), reqBlankHeader);
        assertThat(blankMethodPd.getDetail()).isEqualTo("El método HTTP '' no está soportado para este recurso.");
    }

    @Test
    @DisplayName("Debe manejar MaxUploadSizeExceededException con HTTP 413 RFC 7807 (sin retornar 500)")
    void shouldHandleMaxUploadSizeExceededWith413Rfc7807() {
        MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, "corr-upload-413");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/second-hand/photos");
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(5L * 1024 * 1024);

        ProblemDetail problem = handler.handleMaxUploadSizeExceeded(ex, request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE.value());
        assertThat(problem.getType()).hasToString("https://api.styleradar.com/errors/payload-too-large");
        assertThat(problem.getTitle()).isEqualTo("Archivo demasiado grande");
        assertThat(problem.getDetail()).contains("5 MB");
        assertThat(problem.getInstance().getPath()).isEqualTo("/api/v1/second-hand/photos");
        assertThat(problem.getProperties())
                .containsEntry("code", "PAYLOAD_TOO_LARGE")
                .containsEntry("correlationId", "corr-upload-413")
                .containsKeys("errors", "timestamp");
    }
}
