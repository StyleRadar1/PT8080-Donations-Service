package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import edu.dosw.styleradar.donaciones.domain.exception.ConcurrentDonationClaimException;
import edu.dosw.styleradar.donaciones.domain.exception.DomainConflictException;
import edu.dosw.styleradar.donaciones.domain.exception.ExternalServiceUnavailableException;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidDonationRequestException;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidPublicationException;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidStateTransitionException;
import edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Controlador de excepciones global que implementa la especificación RFC 7807 (Problem Details for HTTP APIs),
 * incluyendo código funcional estable {@code code}, colección {@code errors} y marca temporal {@code timestamp}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TIMESTAMP_PROPERTY = "timestamp";
    private static final String CODE_PROPERTY = "code";
    private static final String ERRORS_PROPERTY = "errors";

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        return buildProblem(HttpStatus.FORBIDDEN, "Acceso denegado",
                "https://styleradar.dosw.edu/errors/forbidden", ex.getMessage(),
                "FORBIDDEN", List.of(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return buildProblem(HttpStatus.NOT_FOUND, "Recurso no encontrado",
                "https://styleradar.dosw.edu/errors/not-found", ex.getMessage(),
                "RESOURCE_NOT_FOUND", List.of(), request);
    }

    @ExceptionHandler({
            InvalidPublicationException.class,
            InvalidDonationRequestException.class,
            IllegalArgumentException.class,
            BadCredentialsException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ProblemDetail handleBadRequest(Exception ex, HttpServletRequest request) {
        log.warn("Solicitud inválida de dominio: {}", ex.getMessage());
        return buildProblem(HttpStatus.BAD_REQUEST, "Solicitud inválida",
                "https://styleradar.dosw.edu/errors/bad-request", ex.getMessage(),
                "BAD_REQUEST", List.of(), request);
    }

    @ExceptionHandler({
            DomainConflictException.class,
            InvalidStateTransitionException.class,
            ConcurrentDonationClaimException.class,
            OptimisticLockingFailureException.class
    })
    public ProblemDetail handleConflict(RuntimeException ex, HttpServletRequest request) {
        log.warn("Conflicto en estado o regla de negocio: {}", ex.getMessage());
        return buildProblem(HttpStatus.CONFLICT, "Conflicto de dominio",
                "https://styleradar.dosw.edu/errors/conflict", ex.getMessage(),
                "DOMAIN_CONFLICT", List.of(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Error de validación en request: {}", ex.getMessage());
        Map<String, String> fieldErrors = new HashMap<>();
        List<Map<String, String>> structuredErrors = new java.util.ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            String message = fieldError.getDefaultMessage();
            fieldErrors.put(fieldError.getField(), message);
            structuredErrors.add(Map.of(
                    "field", fieldError.getField(),
                    "message", message != null ? message : "Valor inválido"
            ));
        }

        ProblemDetail problem = buildProblem(HttpStatus.BAD_REQUEST, "Error de validación",
                "https://styleradar.dosw.edu/errors/validation-error",
                "Los datos enviados contienen errores de validación",
                "VALIDATION_ERROR", structuredErrors, request);
        problem.setProperty("invalidParams", fieldErrors);
        return problem;
    }

    @ExceptionHandler(ExternalServiceUnavailableException.class)
    public ProblemDetail handleExternalServiceUnavailable(ExternalServiceUnavailableException ex,
                                                          HttpServletRequest request) {
        log.error("Servicio externo no disponible: {}", ex.getMessage(), ex);
        return buildProblem(HttpStatus.SERVICE_UNAVAILABLE, "Servicio externo no disponible",
                "https://styleradar.dosw.edu/errors/service-unavailable", ex.getMessage(),
                "SERVICE_UNAVAILABLE", List.of(), request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneralException(Exception ex, HttpServletRequest request) {
        return handleUnhandledException(ex, request);
    }

    public ProblemDetail handleUnhandledException(Exception ex, HttpServletRequest request) {
        log.error("Excepción no controlada detectada en el servidor: ", ex);
        return buildProblem(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor",
                "https://styleradar.dosw.edu/errors/internal-server-error",
                "Ha ocurrido un error inesperado en el servidor",
                "INTERNAL_SERVER_ERROR", List.of(), request);
    }

    private ProblemDetail buildProblem(HttpStatus status, String title, String typeUri,
                                       String detail, String code,
                                       List<Map<String, String>> errors,
                                       HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(typeUri));
        if (request != null && request.getRequestURI() != null) {
            problem.setInstance(URI.create(request.getRequestURI()));
        }
        problem.setProperty(CODE_PROPERTY, code);
        problem.setProperty(ERRORS_PROPERTY, errors != null ? errors : List.of());
        problem.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return problem;
    }
}
