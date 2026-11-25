package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.external;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import edu.dosw.styleradar.donaciones.domain.exception.ExternalServiceUnavailableException;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidDonationRequestException;
import edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException;
import edu.dosw.styleradar.donaciones.domain.model.FoundationSummary;
import edu.dosw.styleradar.donaciones.domain.port.out.FoundationCatalogPort;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.external.dto.TiendasFoundationResponseDTO;
import edu.dosw.styleradar.donaciones.infrastructure.config.TiendasProperties;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Adaptador HTTP hacia ms-tiendas para listar fundaciones aliadas activas
 * y verificar su existencia/estado antes de aceptar donaciones (SR-RF-44).
 * Prioriza los endpoints internos con Basic Auth ({@code TIENDAS_INTERNAL_PASSWORD})
 * y aplica fallback controlado cuando faltan credenciales internas o el endpoint interno falla.
 */
@Component
public class TiendasFoundationCatalogAdapter implements FoundationCatalogPort {

    private static final Logger log = LoggerFactory.getLogger(TiendasFoundationCatalogAdapter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String REPRESENTATIVE_FALLBACK_PATH = "/api/v1/foundations/representative";

    private final RestClient tiendasRestClient;
    private final TiendasProperties tiendasProperties;

    public TiendasFoundationCatalogAdapter(RestClient tiendasRestClient, TiendasProperties tiendasProperties) {
        this.tiendasRestClient = tiendasRestClient;
        this.tiendasProperties = tiendasProperties;
    }

    @Override
    public List<FoundationSummary> findAllActiveFoundations() {
        return fetchAllFoundations().stream()
                .filter(FoundationSummary::activa)
                .toList();
    }

    private List<FoundationSummary> fetchAllFoundations() {
        try {
            TiendasFoundationResponseDTO[] response = tiendasRestClient.get()
                    .uri(tiendasProperties.getFoundationsPath())
                    .headers(this::applyAuthHeaders)
                    .retrieve()
                    .body(TiendasFoundationResponseDTO[].class);

            if (response == null) {
                return List.of();
            }

            return Arrays.stream(response)
                    .map(this::toDomain)
                    .toList();
        } catch (RestClientException ex) {
            throw handleTiendasFailure(
                    "el catálogo de fundaciones",
                    "No fue posible obtener el listado de fundaciones desde ms-tiendas. Intente nuevamente más tarde.",
                    ex);
        }
    }

    @Override
    public Optional<FoundationSummary> findFoundationById(Long foundationId) {
        if (foundationId == null || !tiendasProperties.isValidationEnabled()) {
            return Optional.empty();
        }

        if (!hasText(tiendasProperties.getFoundationByIdPath())) {
            return findFoundationByIdFromList(foundationId);
        }

        if (!hasInternalCredentials()) {
            log.warn("Credenciales internas de ms-tiendas no configuradas (verifique TIENDAS_INTERNAL_PASSWORD); "
                    + "usando fallback de listado general para consultar la fundación ID: {}", foundationId);
            return findFoundationByIdFromList(foundationId);
        }

        try {
            return fetchSingleFoundation(
                    tiendasProperties.getFoundationByIdPath(),
                    foundationId,
                    "Fundación ID: {} no encontrada en ms-tiendas"
            );
        } catch (HttpClientErrorException ex) {
            if (shouldFallbackFromInternal(ex)) {
                log.warn("Acceso rechazado o error {} en endpoint interno de ms-tiendas para fundación ID: {} "
                        + "(verifique TIENDAS_INTERNAL_PASSWORD). Usando fallback de listado general.",
                        ex.getStatusCode().value(), foundationId);
                return findFoundationByIdFromList(foundationId);
            }
            throw handleTiendasFailure(
                    "la fundación ID: " + foundationId,
                    "No fue posible validar la fundación con ms-tiendas. Intente nuevamente más tarde.",
                    ex);
        } catch (RestClientException ex) {
            if (isServerOrConnectivityFailure(ex)) {
                log.warn("Fallo en endpoint interno de ms-tiendas para fundación ID: {} "
                        + "(verifique TIENDAS_INTERNAL_PASSWORD o disponibilidad): {}. Usando fallback de listado general.",
                        foundationId, ex.getMessage());
                return findFoundationByIdFromList(foundationId);
            }
            throw handleTiendasFailure(
                    "la fundación ID: " + foundationId,
                    "No fue posible validar la fundación con ms-tiendas. Intente nuevamente más tarde.",
                    ex);
        }
    }

    @Override
    public Optional<FoundationSummary> findFoundationByRepresentativeId(Long representativeId) {
        if (representativeId == null || !tiendasProperties.isValidationEnabled()) {
            return Optional.empty();
        }

        if (!hasText(tiendasProperties.getFoundationByRepresentativePath())) {
            return fetchAllFoundations().stream()
                    .filter(foundation -> representativeId.equals(foundation.representanteId()))
                    .findFirst();
        }

        if (!hasInternalCredentials()) {
            log.warn("Credenciales internas de ms-tiendas no configuradas (verifique TIENDAS_INTERNAL_PASSWORD); "
                    + "usando fallback {} con Bearer entrante para representante ID: {}",
                    REPRESENTATIVE_FALLBACK_PATH, representativeId);
            return fetchRepresentativeFoundationWithIncomingBearer(representativeId);
        }

        try {
            return fetchSingleFoundation(
                    tiendasProperties.getFoundationByRepresentativePath(),
                    representativeId,
                    "No se encontró fundación asociada al representante ID: {} en ms-tiendas"
            );
        } catch (HttpClientErrorException ex) {
            if (shouldFallbackFromInternal(ex)) {
                log.warn("Acceso rechazado o error {} en endpoint interno de representante {} en ms-tiendas "
                        + "(verifique TIENDAS_INTERNAL_PASSWORD). Usando fallback {}.",
                        ex.getStatusCode().value(), representativeId, REPRESENTATIVE_FALLBACK_PATH);
                return fetchRepresentativeFoundationWithIncomingBearer(representativeId);
            }
            throw handleTiendasFailure(
                    "la fundación del representante ID: " + representativeId,
                    "No fue posible consultar la fundación del representante en ms-tiendas. Intente nuevamente más tarde.",
                    ex);
        } catch (RestClientException ex) {
            if (isServerOrConnectivityFailure(ex)) {
                log.warn("Fallo en endpoint interno de representante {} en ms-tiendas "
                        + "(verifique TIENDAS_INTERNAL_PASSWORD o disponibilidad): {}. Usando fallback {}.",
                        representativeId, ex.getMessage(), REPRESENTATIVE_FALLBACK_PATH);
                return fetchRepresentativeFoundationWithIncomingBearer(representativeId);
            }
            throw handleTiendasFailure(
                    "la fundación del representante ID: " + representativeId,
                    "No fue posible consultar la fundación del representante en ms-tiendas. Intente nuevamente más tarde.",
                    ex);
        }
    }

    private Optional<FoundationSummary> findFoundationByIdFromList(Long foundationId) {
        return fetchAllFoundations().stream()
                .filter(foundation -> foundationId.equals(foundation.id()))
                .findFirst();
    }

    private Optional<FoundationSummary> fetchRepresentativeFoundationWithIncomingBearer(Long representativeId) {
        Optional<String> incomingBearer = extractIncomingBearerHeader();
        if (incomingBearer.isEmpty()) {
            log.warn("No se encontró token Bearer entrante para ejecutar fallback {} (representante ID: {}). "
                    + "Verifique TIENDAS_INTERNAL_PASSWORD.", REPRESENTATIVE_FALLBACK_PATH, representativeId);
            throw new ExternalServiceUnavailableException(
                    "No fue posible consultar la fundación del representante en ms-tiendas. Intente nuevamente más tarde.");
        }
        try {
            TiendasFoundationResponseDTO response = tiendasRestClient.get()
                    .uri(REPRESENTATIVE_FALLBACK_PATH)
                    .header(HttpHeaders.AUTHORIZATION, incomingBearer.get())
                    .retrieve()
                    .body(TiendasFoundationResponseDTO.class);

            return Optional.ofNullable(response).map(this::toDomain);
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("No se encontró fundación asociada al representante ID: {} en ms-tiendas (fallback)", representativeId);
            return Optional.empty();
        } catch (RestClientException ex) {
            throw handleTiendasFailure(
                    "la fundación del representante ID: " + representativeId,
                    "No fue posible consultar la fundación del representante en ms-tiendas. Intente nuevamente más tarde.",
                    ex);
        }
    }

    private Optional<FoundationSummary> fetchSingleFoundation(
            String pathTemplate,
            Long identifier,
            String notFoundLogMessage) {
        try {
            TiendasFoundationResponseDTO response = tiendasRestClient.get()
                    .uri(pathTemplate, identifier)
                    .headers(this::applyInternalAuthHeaders)
                    .retrieve()
                    .body(TiendasFoundationResponseDTO.class);

            return Optional.ofNullable(response).map(this::toDomain);
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn(notFoundLogMessage, identifier);
            return Optional.empty();
        }
    }

    private boolean hasInternalCredentials() {
        return hasText(tiendasProperties.getInternalUsername()) && hasText(tiendasProperties.getInternalPassword());
    }

    private static boolean shouldFallbackFromInternal(HttpClientErrorException ex) {
        return ex.getStatusCode() == HttpStatus.UNAUTHORIZED
                || ex.getStatusCode() == HttpStatus.FORBIDDEN
                || ex.getStatusCode().is5xxServerError();
    }

    private static boolean isServerOrConnectivityFailure(RestClientException ex) {
        return ex instanceof HttpServerErrorException || ex instanceof ResourceAccessException;
    }

    private ExternalServiceUnavailableException handleTiendasFailure(
            String target,
            String publicMessage,
            RestClientException ex) {
        if (ex instanceof HttpClientErrorException httpEx
                && (httpEx.getStatusCode() == HttpStatus.UNAUTHORIZED
                || httpEx.getStatusCode() == HttpStatus.FORBIDDEN)) {
            log.error("Acceso rechazado ({}) al consultar {} en ms-tiendas ({}). "
                            + "Verifique el token o mecanismo de autenticación acordado entre microservicios.",
                    httpEx.getStatusCode().value(), target, tiendasProperties.getBaseUrl(), ex);
        } else {
            log.error("Error consultando {} en ms-tiendas ({})",
                    target, tiendasProperties.getBaseUrl(), ex);
        }
        return new ExternalServiceUnavailableException(publicMessage, ex);
    }

    private void applyInternalAuthHeaders(HttpHeaders headers) {
        headers.setBasicAuth(tiendasProperties.getInternalUsername().trim(), tiendasProperties.getInternalPassword());
    }

    private void applyAuthHeaders(HttpHeaders headers) {
        Optional<String> serviceToken = resolveServiceBearerHeader();
        if (serviceToken.isPresent()) {
            headers.set(HttpHeaders.AUTHORIZATION, serviceToken.get());
        } else if (tiendasProperties.isForwardBearerToken()) {
            extractIncomingBearerHeader().ifPresent(token -> headers.set(HttpHeaders.AUTHORIZATION, token));
        }
        String rolesHeaderName = tiendasProperties.getServiceRolesHeaderName();
        String rolesHeaderValue = tiendasProperties.getServiceRolesHeaderValue();
        if (hasText(rolesHeaderName) && hasText(rolesHeaderValue)) {
            headers.set(rolesHeaderName, rolesHeaderValue);
        }
    }

    private Optional<String> resolveServiceBearerHeader() {
        String raw = tiendasProperties.getServiceBearerToken();
        if (!hasText(raw)) {
            return Optional.empty();
        }
        String trimmed = raw.trim();
        if (trimmed.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return trimmed.length() > BEARER_PREFIX.length() ? Optional.of(trimmed) : Optional.empty();
        }
        return Optional.of(BEARER_PREFIX + trimmed);
    }

    private Optional<String> extractIncomingBearerHeader() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return Optional.empty();
        }
        HttpServletRequest request = attributes.getRequest();
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null
                && authHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())
                && authHeader.trim().length() > BEARER_PREFIX.length()) {
            return Optional.of(authHeader.trim());
        }
        return Optional.empty();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Override
    public void validateActiveFoundation(Long foundationId) {
        if (foundationId == null) {
            throw new InvalidDonationRequestException("El ID de la fundación es obligatorio");
        }
        if (!tiendasProperties.isValidationEnabled()) {
            log.debug("Validación remota contra ms-tiendas deshabilitada por configuración para fundación ID: {}",
                    foundationId);
            return;
        }

        FoundationSummary foundation = findFoundationById(foundationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una fundación aliada registrada con ID: " + foundationId));

        if (!foundation.activa()) {
            throw new InvalidDonationRequestException(
                    "La fundación '" + foundation.nombre() + "' (ID: " + foundationId
                            + ") no se encuentra activa para recibir donaciones");
        }
    }

    @Override
    public boolean isFoundationRepresentative(Long userId, Long foundationId) {
        if (userId == null || foundationId == null || !tiendasProperties.isValidationEnabled()) {
            return false;
        }
        return findFoundationById(foundationId)
                .map(foundation -> foundation.representanteId() != null && foundation.representanteId().equals(userId))
                .orElse(false);
    }

    private FoundationSummary toDomain(TiendasFoundationResponseDTO dto) {
        return new FoundationSummary(
                dto.id(),
                dto.nombre(),
                dto.barrio(),
                dto.descripcion(),
                dto.direccion(),
                dto.telefono(),
                dto.email(),
                dto.categoriasAceptadas(),
                dto.causasApoyadas(),
                dto.horarioAtencion(),
                dto.politicasDonacion(),
                Boolean.TRUE.equals(dto.activa()),
                dto.representanteId()
        );
    }
}
