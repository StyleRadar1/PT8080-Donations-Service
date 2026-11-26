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
 * Adaptador HTTP hacia ms-tiendas para listar fundaciones aliadas activas (catálogo público)
 * y verificar autorización/propiedad mediante endpoints autenticados que exponen {@code representativeId}.
 * Separa estrictamente las consultas de catálogo público de la verificación RBAC de propiedad.
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

    /**
     * Consulta una fundación para visualización o validación de existencia/estado activo.
     * Solo ante indisponibilidad del endpoint interno puede degradar al listado público de catálogo
     * (el cual no expone {@code representativeId}).
     */
    @Override
    public Optional<FoundationSummary> findFoundationById(Long foundationId) {
        if (foundationId == null || !tiendasProperties.isValidationEnabled()) {
            return Optional.empty();
        }

        if (!hasText(tiendasProperties.getFoundationByIdPath())) {
            return findFoundationByIdFromPublicCatalog(foundationId);
        }

        if (!hasInternalCredentials()) {
            log.warn("Credenciales internas de ms-tiendas no configuradas; usando catálogo público de solo lectura para consultar existencia de fundación ID: {}",
                    foundationId);
            return findFoundationByIdFromPublicCatalog(foundationId);
        }

        try {
            return fetchSingleFoundationInternal(
                    tiendasProperties.getFoundationByIdPath(),
                    foundationId,
                    "Fundación ID: {} no encontrada en ms-tiendas"
            );
        } catch (HttpClientErrorException ex) {
            if (shouldFallbackFromInternal(ex)) {
                log.warn("Error {} en endpoint interno de ms-tiendas para consulta pública de fundación ID: {}. "
                        + "Usando catálogo público (sin representativeId).",
                        ex.getStatusCode().value(), foundationId);
                return findFoundationByIdFromPublicCatalog(foundationId);
            }
            throw handleTiendasFailure(
                    "la fundación ID: " + foundationId,
                    "No fue posible validar la fundación con ms-tiendas. Intente nuevamente más tarde.",
                    ex);
        } catch (RestClientException ex) {
            if (isServerOrConnectivityFailure(ex)) {
                log.warn("Fallo en endpoint interno de ms-tiendas para fundación ID: {}: {}. Usando catálogo público (sin representativeId).",
                        foundationId, ex.getMessage());
                return findFoundationByIdFromPublicCatalog(foundationId);
            }
            throw handleTiendasFailure(
                    "la fundación ID: " + foundationId,
                    "No fue posible validar la fundación con ms-tiendas. Intente nuevamente más tarde.",
                    ex);
        }
    }

    /**
     * Resuelve la fundación de un representante utilizando exclusivamente endpoints autenticados
     * que garantizan la presencia de {@code representativeId}. Nunca consulta el listado público.
     */
    @Override
    public Optional<FoundationSummary> findFoundationByRepresentativeId(Long representativeId) {
        if (representativeId == null || !tiendasProperties.isValidationEnabled()) {
            return Optional.empty();
        }

        if (!hasText(tiendasProperties.getFoundationByRepresentativePath())) {
            return fetchRepresentativeFoundationWithIncomingBearer(representativeId);
        }

        if (!hasInternalCredentials()) {
            return fetchRepresentativeFoundationWithIncomingBearer(representativeId);
        }

        try {
            return fetchSingleFoundationInternal(
                    tiendasProperties.getFoundationByRepresentativePath(),
                    representativeId,
                    "No se encontró fundación asociada al representante ID: {} en ms-tiendas"
            );
        } catch (HttpClientErrorException ex) {
            if (shouldFallbackFromInternal(ex)) {
                log.warn("Acceso rechazado o error {} en endpoint interno de representante {} en ms-tiendas. "
                        + "Intentando verificación autenticada en {}.",
                        ex.getStatusCode().value(), representativeId, REPRESENTATIVE_FALLBACK_PATH);
                return fetchRepresentativeFoundationWithIncomingBearer(representativeId);
            }
            throw handleTiendasFailure(
                    "la fundación del representante ID: " + representativeId,
                    "No fue posible consultar la fundación del representante en ms-tiendas. Intente nuevamente más tarde.",
                    ex);
        } catch (RestClientException ex) {
            if (isServerOrConnectivityFailure(ex)) {
                log.warn("Fallo en endpoint interno de representante {} en ms-tiendas: {}. "
                        + "Intentando verificación autenticada en {}.",
                        representativeId, ex.getMessage(), REPRESENTATIVE_FALLBACK_PATH);
                return fetchRepresentativeFoundationWithIncomingBearer(representativeId);
            }
            throw handleTiendasFailure(
                    "la fundación del representante ID: " + representativeId,
                    "No fue posible consultar la fundación del representante en ms-tiendas. Intente nuevamente más tarde.",
                    ex);
        }
    }

    private Optional<FoundationSummary> findFoundationByIdFromPublicCatalog(Long foundationId) {
        return fetchAllFoundations().stream()
                .filter(foundation -> foundationId.equals(foundation.id()))
                .findFirst();
    }

    private Optional<FoundationSummary> fetchRepresentativeFoundationWithIncomingBearer(Long representativeId) {
        Optional<String> incomingBearer = extractIncomingBearerHeader();
        if (incomingBearer.isEmpty()) {
            log.error("Sin credenciales internas ni token Bearer entrante para verificar representante ID: {} en ms-tiendas.",
                    representativeId);
            throw new ExternalServiceUnavailableException(
                    "No fue posible verificar la representación de la fundación en ms-tiendas. Intente nuevamente más tarde.");
        }
        try {
            TiendasFoundationResponseDTO response = tiendasRestClient.get()
                    .uri(REPRESENTATIVE_FALLBACK_PATH)
                    .header(HttpHeaders.AUTHORIZATION, incomingBearer.get())
                    .retrieve()
                    .body(TiendasFoundationResponseDTO.class);

            return Optional.ofNullable(response)
                    .map(this::toDomain)
                    .filter(foundation -> foundation.representanteId() != null
                            && representativeId.equals(foundation.representanteId()));
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("No se encontró fundación asociada al representante ID: {} en ms-tiendas (consulta autenticada)", representativeId);
            return Optional.empty();
        } catch (RestClientException ex) {
            throw handleTiendasFailure(
                    "la fundación del representante ID: " + representativeId,
                    "No fue posible consultar la fundación del representante en ms-tiendas. Intente nuevamente más tarde.",
                    ex);
        }
    }

    private Optional<FoundationSummary> fetchSingleFoundationInternal(
            String pathTemplate,
            Long identifier,
            String notFoundLogMessage) {
        ensureInternalCredentialsConfigured("el endpoint interno de ms-tiendas");
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

    private void ensureInternalCredentialsConfigured(String target) {
        if (!hasInternalCredentials()) {
            log.error("Credenciales Basic internas de ms-tiendas no configuradas para {} "
                            + "(styleradar.tiendas.internal-username / internal-password). Se cancela la llamada saliente.",
                    target);
            throw new ExternalServiceUnavailableException(
                    "La integración interna con ms-tiendas no se encuentra configurada correctamente. Intente nuevamente más tarde.");
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
        ensureInternalCredentialsConfigured("el endpoint interno de ms-tiendas");
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

    /**
     * Verifica la propiedad de una fundación exclusivamente contra fuentes autenticadas que expongan
     * {@code representativeId}, sin usar nunca el catálogo público {@code /api/v1/foundations}.
     */
    @Override
    public boolean isFoundationRepresentative(Long userId, Long foundationId) {
        if (userId == null || foundationId == null || !tiendasProperties.isValidationEnabled()) {
            return false;
        }

        Optional<FoundationSummary> internalFoundation = tryFetchInternalFoundationForAuthorization(foundationId);
        if (internalFoundation.isPresent()) {
            FoundationSummary foundation = internalFoundation.get();
            return foundation.representanteId() != null && foundation.representanteId().equals(userId);
        }

        return false;
    }

    private Optional<FoundationSummary> tryFetchInternalFoundationForAuthorization(Long foundationId) {
        if (hasText(tiendasProperties.getFoundationByIdPath()) && hasInternalCredentials()) {
            try {
                Optional<FoundationSummary> direct = fetchSingleFoundationInternal(
                        tiendasProperties.getFoundationByIdPath(),
                        foundationId,
                        "Fundación ID: {} no encontrada en ms-tiendas durante verificación de propiedad"
                );
                if (direct.isPresent() && direct.get().representanteId() != null) {
                    return direct;
                }
                if (direct.isEmpty()) {
                    return Optional.empty();
                }
            } catch (RestClientException ex) {
                if (!(ex instanceof HttpClientErrorException httpEx && shouldFallbackFromInternal(httpEx))
                        && !isServerOrConnectivityFailure(ex)) {
                    throw handleTiendasFailure(
                            "la autorización sobre la fundación ID: " + foundationId,
                            "No fue posible verificar la propiedad de la fundación en ms-tiendas. Intente nuevamente más tarde.",
                            ex);
                }
                log.warn("No se pudo verificar propiedad vía endpoint interno para fundación ID: {}; intentando vía representante autenticado.",
                        foundationId);
            }
        }

        if (extractIncomingBearerHeader().isPresent()) {
            return fetchRepresentativeFoundationViaIncomingBearerForFoundation(foundationId);
        }

        throw new ExternalServiceUnavailableException(
                "No fue posible verificar la propiedad de la fundación en ms-tiendas mediante un canal autenticado.");
    }

    private Optional<FoundationSummary> fetchRepresentativeFoundationViaIncomingBearerForFoundation(Long foundationId) {
        Optional<String> incomingBearer = extractIncomingBearerHeader();
        if (incomingBearer.isEmpty()) {
            return Optional.empty();
        }
        try {
            TiendasFoundationResponseDTO response = tiendasRestClient.get()
                    .uri(REPRESENTATIVE_FALLBACK_PATH)
                    .header(HttpHeaders.AUTHORIZATION, incomingBearer.get())
                    .retrieve()
                    .body(TiendasFoundationResponseDTO.class);

            return Optional.ofNullable(response)
                    .map(this::toDomain)
                    .filter(foundation -> foundationId.equals(foundation.id())
                            && foundation.representanteId() != null);
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        } catch (RestClientException ex) {
            throw handleTiendasFailure(
                    "la autorización sobre la fundación ID: " + foundationId,
                    "No fue posible verificar la propiedad de la fundación en ms-tiendas. Intente nuevamente más tarde.",
                    ex);
        }
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
