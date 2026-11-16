package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.external;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import edu.dosw.styleradar.donaciones.domain.exception.ExternalServiceUnavailableException;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidDonationRequestException;
import edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException;
import edu.dosw.styleradar.donaciones.domain.model.FoundationSummary;
import edu.dosw.styleradar.donaciones.domain.port.out.FoundationCatalogPort;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.external.dto.TiendasFoundationResponseDTO;
import edu.dosw.styleradar.donaciones.infrastructure.config.TiendasProperties;

/**
 * Adaptador HTTP hacia ms-tiendas para listar fundaciones aliadas activas
 * y verificar su existencia/estado antes de aceptar donaciones (SR-RF-44).
 */
@Component
public class TiendasFoundationCatalogAdapter implements FoundationCatalogPort {

    private static final Logger log = LoggerFactory.getLogger(TiendasFoundationCatalogAdapter.class);
    private static final String HEADER_USER_ROLES = "X-User-Roles";
    private static final String ROLE_ADMIN_STYLERADAR = "ROLE_ADMIN_STYLERADAR";
    private static final String ADMIN_FOUNDATIONS_PATH = "/api/v1/admin/fundaciones";
    private static final String ADMIN_FOUNDATION_BY_ID_PATH = "/api/v1/admin/fundaciones/{id}";

    private final RestClient tiendasRestClient;
    private final TiendasProperties tiendasProperties;

    public TiendasFoundationCatalogAdapter(RestClient tiendasRestClient, TiendasProperties tiendasProperties) {
        this.tiendasRestClient = tiendasRestClient;
        this.tiendasProperties = tiendasProperties;
    }

    @Override
    public List<FoundationSummary> findAllActiveFoundations() {
        try {
            TiendasFoundationResponseDTO[] response = tiendasRestClient.get()
                    .uri(ADMIN_FOUNDATIONS_PATH)
                    .header(HEADER_USER_ROLES, ROLE_ADMIN_STYLERADAR)
                    .retrieve()
                    .body(TiendasFoundationResponseDTO[].class);

            if (response == null) {
                return List.of();
            }

            return Arrays.stream(response)
                    .map(this::toDomain)
                    .filter(FoundationSummary::activa)
                    .toList();
        } catch (RestClientException ex) {
            log.error("Error consultando el catálogo de fundaciones en ms-tiendas ({})",
                    tiendasProperties.getBaseUrl(), ex);
            throw new ExternalServiceUnavailableException(
                    "No fue posible obtener el listado de fundaciones desde ms-tiendas. Intente nuevamente más tarde.",
                    ex);
        }
    }

    @Override
    public Optional<FoundationSummary> findFoundationById(Long foundationId) {
        if (foundationId == null) {
            return Optional.empty();
        }

        try {
            TiendasFoundationResponseDTO response = tiendasRestClient.get()
                    .uri(ADMIN_FOUNDATION_BY_ID_PATH, foundationId)
                    .header(HEADER_USER_ROLES, ROLE_ADMIN_STYLERADAR)
                    .retrieve()
                    .body(TiendasFoundationResponseDTO.class);

            return Optional.ofNullable(response).map(this::toDomain);
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("Fundación ID: {} no encontrada en ms-tiendas", foundationId);
            return Optional.empty();
        } catch (RestClientException ex) {
            log.error("Error consultando la fundación ID: {} en ms-tiendas ({})",
                    foundationId, tiendasProperties.getBaseUrl(), ex);
            throw new ExternalServiceUnavailableException(
                    "No fue posible validar la fundación con ms-tiendas. Intente nuevamente más tarde.",
                    ex);
        }
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
                Boolean.TRUE.equals(dto.activa())
        );
    }
}
