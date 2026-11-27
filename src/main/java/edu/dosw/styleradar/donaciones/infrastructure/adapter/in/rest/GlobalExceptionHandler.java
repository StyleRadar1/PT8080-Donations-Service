package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import edu.dosw.styleradar.donaciones.domain.exception.ConcurrentDonationClaimException;
import edu.dosw.styleradar.donaciones.domain.exception.DomainConflictException;
import edu.dosw.styleradar.donaciones.domain.exception.ExternalServiceUnavailableException;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidDonationRequestException;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidPublicationException;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidStateTransitionException;
import edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest.filter.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Controlador de excepciones global que implementa la especificación RFC 7807 (Problem Details for HTTP APIs),
 * incluyendo código funcional estable {@code code}, colección {@code errors}, {@code correlationId}
 * y marca temporal {@code timestamp}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TIMESTAMP_PROPERTY = "timestamp";
    private static final String CODE_PROPERTY = "code";
    private static final String ERRORS_PROPERTY = "errors";
    private static final String CORRELATION_ID_PROPERTY = "correlationId";
    private static final String ERROR_TYPE_BASE_URI = "https://api.styleradar.com/errors/";

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        return buildProblem(HttpStatus.FORBIDDEN, "Acceso denegado",
                ex.getMessage(), "FORBIDDEN", List.of(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return buildProblem(HttpStatus.NOT_FOUND, "Recurso no encontrado",
                ex.getMessage(), "RESOURCE_NOT_FOUND", List.of(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("Ruta HTTP no encontrada: {}", ex.getResourcePath());
        return buildProblem(HttpStatus.NOT_FOUND, "Recurso no encontrado",
                "El recurso solicitado no existe.", "RESOURCE_NOT_FOUND", List.of(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String paramName = StringUtils.hasText(ex.getName()) ? ex.getName() : "parámetro";
        log.warn("Parámetro '{}' con formato inválido: {}", paramName, ex.getMessage());
        return buildProblem(HttpStatus.BAD_REQUEST, "Parámetro inválido",
                "El parámetro '" + paramName + "' tiene un formato inválido.",
                "BAD_REQUEST", List.of(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Cuerpo de solicitud JSON no legible: {}", ex.getMessage());
        return buildProblem(HttpStatus.BAD_REQUEST, "Cuerpo de solicitud inválido",
                "El cuerpo de la solicitud JSON está malformado o contiene valores inválidos.",
                "MALFORMED_REQUEST_BODY", List.of(), request);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ProblemDetail handleMissingPart(MissingServletRequestPartException ex, HttpServletRequest request) {
        log.warn("Falta parte multipart requerida '{}': {}", ex.getRequestPartName(), ex.getMessage());
        return buildProblem(HttpStatus.BAD_REQUEST, "Parte multipart faltante",
                "Falta la parte requerida '" + ex.getRequestPartName() + "' en la solicitud.",
                "MISSING_REQUEST_PART", List.of(), request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("Archivo o solicitud multipart supera el tamaño máximo permitido: {}", ex.getMessage());
        return buildProblem(HttpStatus.PAYLOAD_TOO_LARGE, "Archivo demasiado grande",
                "La fotografía o solicitud supera el límite máximo permitido (5 MB por archivo / 6 MB por solicitud).",
                "PAYLOAD_TOO_LARGE", List.of(), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String method = StringUtils.hasText(ex.getMethod()) ? ex.getMethod() : "";
        log.warn("Método HTTP no soportado: {}", method);
        return buildProblem(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP no permitido",
                "El método HTTP '" + method + "' no está soportado para este recurso.",
                "METHOD_NOT_ALLOWED", List.of(), request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        log.warn("Tipo de contenido no soportado: {}", ex.getContentType());
        return buildProblem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de contenido no soportado",
                "El tipo de contenido enviado no es compatible con este recurso.",
                "UNSUPPORTED_MEDIA_TYPE", List.of(), request);
    }

    @ExceptionHandler({
            InvalidPublicationException.class,
            InvalidDonationRequestException.class,
            IllegalArgumentException.class,
            BadCredentialsException.class,
            MissingServletRequestParameterException.class
    })
    public ProblemDetail handleBadRequest(Exception ex, HttpServletRequest request) {
        log.warn("Solicitud inválida de dominio: {}", ex.getMessage());
        String detail = ex instanceof MissingServletRequestParameterException missingParam
                ? "Falta el parámetro requerido '" + missingParam.getParameterName() + "' en la solicitud."
                : sanitizeBadRequestMessage(ex.getMessage());
        return buildProblem(HttpStatus.BAD_REQUEST, "Solicitud inválida",
                detail, "BAD_REQUEST", List.of(), request);
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
                ex.getMessage(), "DOMAIN_CONFLICT", List.of(), request);
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
                ex.getMessage(), "SERVICE_UNAVAILABLE", List.of(), request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneralException(Exception ex, HttpServletRequest request) {
        return handleUnhandledException(ex, request);
    }

    public ProblemDetail handleUnhandledException(Exception ex, HttpServletRequest request) {
        log.error("Excepción no controlada detectada en el servidor: ", ex);
        return buildProblem(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor",
                "Ha ocurrido un error inesperado en el servidor",
                "INTERNAL_SERVER_ERROR", List.of(), request);
    }

    private ProblemDetail buildProblem(HttpStatus status, String title,
                                       String detail, String code,
                                       List<Map<String, String>> errors,
                                       HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(resolveProblemTypeUri(code));
        if (request != null && request.getRequestURI() != null) {
            problem.setInstance(URI.create(request.getRequestURI()));
        }
        problem.setProperty(CODE_PROPERTY, code);
        problem.setProperty(ERRORS_PROPERTY, errors != null ? errors : List.of());
        problem.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        String correlationId = resolveCorrelationId(request);
        if (correlationId != null) {
            problem.setProperty(CORRELATION_ID_PROPERTY, correlationId);
        }
        return problem;
    }

    private static URI resolveProblemTypeUri(String code) {
        String slug = (code == null || code.isBlank())
                ? "internal-server-error"
                : code.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        return URI.create(ERROR_TYPE_BASE_URI + slug);
    }

    private static String resolveCorrelationId(HttpServletRequest request) {
        String mdcValue = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
        if (mdcValue != null && !mdcValue.isBlank()) {
            return mdcValue.trim();
        }
        if (request != null) {
            String headerValue = request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
            if (headerValue != null && !headerValue.isBlank()) {
                return headerValue.trim();
            }
        }
        return null;
    }

    private static String sanitizeBadRequestMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return "La solicitud contiene parámetros inválidos.";
        }
        if (rawMessage.contains("java.")
                || rawMessage.contains("edu.dosw.")
                || rawMessage.contains("No enum constant")) {
            return "Uno de los parámetros enviados tiene un formato inválido.";
        }
        return rawMessage;
    }
}
