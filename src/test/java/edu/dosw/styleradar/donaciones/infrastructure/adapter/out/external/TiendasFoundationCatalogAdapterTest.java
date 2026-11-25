package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.external;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import edu.dosw.styleradar.donaciones.domain.exception.ExternalServiceUnavailableException;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidDonationRequestException;
import edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException;
import edu.dosw.styleradar.donaciones.domain.model.FoundationSummary;
import edu.dosw.styleradar.donaciones.infrastructure.config.TiendasClientConfig;
import edu.dosw.styleradar.donaciones.infrastructure.config.TiendasProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("Pruebas Unitarias - TiendasFoundationCatalogAdapter")
class TiendasFoundationCatalogAdapterTest {

    private static final String BASE_URL = "http://localhost:8140";

    private MockRestServiceServer mockServer;
    private TiendasProperties properties;
    private TiendasFoundationCatalogAdapter adapter;

    @BeforeEach
    void setUp() {
        properties = new TiendasProperties();
        properties.setBaseUrl(BASE_URL);
        properties.setFoundationsPath("/api/v1/admin/foundations");
        properties.setFoundationByIdPath("/api/v1/admin/foundations/{id}");
        properties.setValidationEnabled(true);
        properties.setConnectTimeoutMs(1000);
        properties.setReadTimeoutMs(1000);

        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();

        RestClient restClient = builder.build();
        adapter = new TiendasFoundationCatalogAdapter(restClient, properties);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Debe consultar listado en ms-tiendas con header administrativo y filtrar solo fundaciones activas")
    void shouldFetchAndFilterActiveFoundations() {
        String json = """
                [
                  {
                    "id": 3,
                    "nombre": "Fundación Abrigo Solidario",
                    "nit": "900215884-1",
                    "barrio": "Chapinero",
                    "descripcion": "Recolección de prendas",
                    "direccion": "Calle 85 # 15-32",
                    "telefono": "+57 601 742 8899",
                    "email": "contacto@abrigosolidario.org",
                    "categoriasAceptadas": ["SUPERIOR", "INFERIOR"],
                    "causasApoyadas": "Niñez vulnerable",
                    "horarioAtencion": "Lunes a Sábado 08:00 a 17:00",
                    "politicasDonacion": "Prendas limpias",
                    "activa": true
                  },
                  {
                    "id": 9,
                    "nombre": "Fundación Suspendida",
                    "activa": false
                  }
                ]
                """;

        mockServer.expect(requestTo(BASE_URL + "/api/v1/admin/foundations"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-User-Roles", "ROLE_ADMIN_STYLERADAR"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<FoundationSummary> result = adapter.findAllActiveFoundations();

        mockServer.verify();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(3L);
        assertThat(result.get(0).nombre()).isEqualTo("Fundación Abrigo Solidario");
        assertThat(result.get(0).categoriasAceptadas()).containsExactly("SUPERIOR", "INFERIOR");
    }

    @Test
    @DisplayName("Debe validar exitosamente una fundación activa por ID en ms-tiendas")
    void shouldValidateActiveFoundationSuccessfully() {
        String json = """
                {
                  "id": 3,
                  "nombre": "Fundación Abrigo Solidario",
                  "activa": true
                }
                """;

        mockServer.expect(requestTo(BASE_URL + "/api/v1/admin/foundations/3"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-User-Roles", "ROLE_ADMIN_STYLERADAR"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        assertThatCode(() -> adapter.validateActiveFoundation(3L)).doesNotThrowAnyException();
        mockServer.verify();
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException cuando ms-tiendas responde 404 para el ID")
    void shouldThrowNotFoundWhenTiendasReturns404() {
        mockServer.expect(requestTo(BASE_URL + "/api/v1/admin/foundations/999"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withResourceNotFound());

        assertThatThrownBy(() -> adapter.validateActiveFoundation(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        mockServer.verify();
    }

    @Test
    @DisplayName("Debe lanzar InvalidDonationRequestException cuando la fundación tiene activa=false")
    void shouldThrowBadRequestWhenFoundationIsInactive() {
        String json = """
                {
                  "id": 4,
                  "nombre": "Fundación Inactiva",
                  "activa": false
                }
                """;

        mockServer.expect(requestTo(BASE_URL + "/api/v1/admin/foundations/4"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.validateActiveFoundation(4L))
                .isInstanceOf(InvalidDonationRequestException.class);
        mockServer.verify();
    }

    @Test
    @DisplayName("Debe lanzar ExternalServiceUnavailableException cuando ms-tiendas falla con error 500")
    void shouldThrowExternalServiceUnavailableOnServerError() {
        mockServer.expect(requestTo(BASE_URL + "/api/v1/admin/foundations/3"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertThatThrownBy(() -> adapter.validateActiveFoundation(3L))
                .isInstanceOf(ExternalServiceUnavailableException.class);
        mockServer.verify();
    }

    @Test
    @DisplayName("Debe omitir llamada HTTP remota cuando validationEnabled es false")
    void shouldSkipRemoteCallWhenValidationDisabled() {
        properties.setValidationEnabled(false);

        assertThatCode(() -> adapter.validateActiveFoundation(3L)).doesNotThrowAnyException();
        mockServer.verify();
    }

    @Test
    @DisplayName("Debe retornar Optional.empty() si findFoundationById recibe null")
    void shouldReturnEmptyWhenIdIsNull() {
        Optional<FoundationSummary> result = adapter.findFoundationById(null);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Debe construir bean RestClient configurado con baseUrl y timeouts")
    void shouldBuildConfiguredTiendasRestClient() {
        RestClient configured = new TiendasClientConfig().tiendasRestClient(RestClient.builder(), properties);
        assertThat(configured).isNotNull();
    }

    @Test
    @DisplayName("Debe verificar correctamente si el usuario es representanteId de la fundación en ms-tiendas")
    void shouldValidateFoundationRepresentative() {
        String json = """
                {
                  "id": 3,
                  "nombre": "Fundación Abrigo Solidario",
                  "representanteId": 24,
                  "activa": true
                }
                """;

        mockServer.expect(requestTo(BASE_URL + "/api/v1/admin/foundations/3"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        assertThat(adapter.isFoundationRepresentative(24L, 3L)).isTrue();
        mockServer.verify();

        mockServer.reset();
        mockServer.expect(requestTo(BASE_URL + "/api/v1/admin/foundations/3"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        assertThat(adapter.isFoundationRepresentative(999L, 3L)).isFalse();
        mockServer.verify();

        // Ramas nulas y validationEnabled=false
        assertThat(adapter.isFoundationRepresentative(null, 3L)).isFalse();
        assertThat(adapter.isFoundationRepresentative(24L, null)).isFalse();
        assertThatThrownBy(() -> adapter.validateActiveFoundation(null))
                .isInstanceOf(InvalidDonationRequestException.class);

        mockServer.reset();
        mockServer.expect(requestTo(BASE_URL + "/api/v1/admin/foundations"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));
        assertThat(adapter.findAllActiveFoundations()).isEmpty();
        mockServer.verify();

        properties.setValidationEnabled(false);
        assertThat(adapter.isFoundationRepresentative(24L, 3L)).isFalse();
    }

    @Test
    @DisplayName("Por defecto NO debe propagar el Bearer del donante hacia /api/v1/admin/foundations y sí enviar serviceBearerToken cuando está configurado")
    void shouldUseServiceAuthWithoutLeakingDonorBearerToken() {
        MockHttpServletRequest donorRequest = new MockHttpServletRequest();
        donorRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer donor-user-jwt");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(donorRequest));

        String listJson = """
                [
                  {
                    "id": 3,
                    "nombre": "Fundación Abrigo Solidario",
                    "activa": true,
                    "representanteId": 24
                  }
                ]
                """;
        String byIdJson = """
                {
                  "id": 3,
                  "nombre": "Fundación Abrigo Solidario",
                  "activa": true,
                  "representanteId": 24
                }
                """;

        // 1. Por defecto (forwardBearerToken=false, serviceBearerToken vacío): nunca propaga el token del donante
        mockServer.expect(requestTo(BASE_URL + "/api/v1/admin/foundations"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION))
                .andExpect(header("X-User-Roles", "ROLE_ADMIN_STYLERADAR"))
                .andRespond(withSuccess(listJson, MediaType.APPLICATION_JSON));

        assertThat(adapter.findAllActiveFoundations()).hasSize(1);
        mockServer.verify();

        // 2. Con serviceBearerToken configurado: tiene precedencia sobre cualquier token del donante
        mockServer.reset();
        properties.setForwardBearerToken(true);
        properties.setServiceBearerToken("svc-admin-jwt-999");

        mockServer.expect(requestTo(BASE_URL + "/api/v1/admin/foundations/3"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer svc-admin-jwt-999"))
                .andExpect(header("X-User-Roles", "ROLE_ADMIN_STYLERADAR"))
                .andRespond(withSuccess(byIdJson, MediaType.APPLICATION_JSON));

        assertThat(adapter.findFoundationById(3L)).isPresent();
        mockServer.verify();

        // 3. Con forwardBearerToken=true explícito y sin serviceBearerToken: reenvía el token entrante
        mockServer.reset();
        properties.setServiceBearerToken("");
        properties.setForwardBearerToken(true);

        mockServer.expect(requestTo(BASE_URL + "/api/v1/admin/foundations"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer donor-user-jwt"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(adapter.findAllActiveFoundations()).isEmpty();
        mockServer.verify();

        // 4. Con cabecera de rol vacía y forwardBearerToken=false: no envía Authorization ni X-User-Roles
        mockServer.reset();
        properties.setForwardBearerToken(false);
        properties.setServiceBearerToken("Bearer explicit-prefixed-token");
        properties.setServiceRolesHeaderValue("");

        mockServer.expect(requestTo(BASE_URL + "/api/v1/admin/foundations"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer explicit-prefixed-token"))
                .andExpect(headerDoesNotExist("X-User-Roles"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(adapter.findAllActiveFoundations()).isEmpty();
        mockServer.verify();
    }

    @Test
    @DisplayName("Debe mapear 401 y 403 de ms-tiendas a ExternalServiceUnavailableException (503) sin falsos 404")
    void shouldMapUnauthorizedAndForbiddenFromTiendasTo503() {
        mockServer.expect(requestTo(BASE_URL + "/api/v1/admin/foundations"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> adapter.findAllActiveFoundations())
                .isInstanceOf(ExternalServiceUnavailableException.class);
        mockServer.verify();

        mockServer.reset();
        mockServer.expect(requestTo(BASE_URL + "/api/v1/admin/foundations/3"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> adapter.findFoundationById(3L))
                .isInstanceOf(ExternalServiceUnavailableException.class);
        mockServer.verify();
    }

    @Test
    @DisplayName("Debe deserializar el contrato en inglés de ms-tiendas QA y resolver por ID usando /api/v1/foundations cuando foundationByIdPath está vacío")
    void shouldDeserializeEnglishPayloadAndLookupByIdViaFoundationsList() {
        properties.setFoundationsPath("/api/v1/foundations");
        properties.setFoundationByIdPath("");

        String englishListJson = """
                [
                  {
                    "id": 1,
                    "nit": "900777888-1",
                    "email": "fundacion.qa@styleradar.com",
                    "name": "Fundación Pruebas QA",
                    "neighborhood": "Chapinero",
                    "description": "Fundación de prueba para integración",
                    "address": "Calle 85 # 15-32, Bogotá",
                    "phone": "+57 601 742 8899",
                    "acceptedCategories": ["FOOTWEAR", "TOP", "BOTTOM"],
                    "representativeId": 901,
                    "supportedCauses": "Moda circular",
                    "openingHours": "08:00-17:00",
                    "donationPolicies": "Ropa limpia",
                    "active": true
                  }
                ]
                """;

        mockServer.expect(requestTo(BASE_URL + "/api/v1/foundations"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(englishListJson, MediaType.APPLICATION_JSON));

        Optional<FoundationSummary> found = adapter.findFoundationById(1L);
        mockServer.verify();

        assertThat(found).isPresent();
        assertThat(found.get().nombre()).isEqualTo("Fundación Pruebas QA");
        assertThat(found.get().barrio()).isEqualTo("Chapinero");
        assertThat(found.get().activa()).isTrue();
        assertThat(found.get().representanteId()).isEqualTo(901L);
        assertThat(found.get().categoriasAceptadas()).containsExactly("FOOTWEAR", "TOP", "BOTTOM");

        mockServer.reset();
        mockServer.expect(requestTo(BASE_URL + "/api/v1/foundations"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(englishListJson, MediaType.APPLICATION_JSON));

        assertThat(adapter.findFoundationById(999L)).isEmpty();
        mockServer.verify();
    }
}
