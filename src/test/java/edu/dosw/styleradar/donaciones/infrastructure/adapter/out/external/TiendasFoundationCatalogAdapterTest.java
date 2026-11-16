package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.external;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
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
        properties.setValidationEnabled(true);
        properties.setConnectTimeoutMs(1000);
        properties.setReadTimeoutMs(1000);

        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();

        RestClient restClient = builder.build();
        adapter = new TiendasFoundationCatalogAdapter(restClient, properties);
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

        mockServer.expect(requestTo(BASE_URL + "/api/v1/admin/fundaciones"))
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

        mockServer.expect(requestTo(BASE_URL + "/api/v1/admin/fundaciones/3"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-User-Roles", "ROLE_ADMIN_STYLERADAR"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        assertThatCode(() -> adapter.validateActiveFoundation(3L)).doesNotThrowAnyException();
        mockServer.verify();
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException cuando ms-tiendas responde 404 para el ID")
    void shouldThrowNotFoundWhenTiendasReturns404() {
        mockServer.expect(requestTo(BASE_URL + "/api/v1/admin/fundaciones/999"))
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

        mockServer.expect(requestTo(BASE_URL + "/api/v1/admin/fundaciones/4"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.validateActiveFoundation(4L))
                .isInstanceOf(InvalidDonationRequestException.class);
        mockServer.verify();
    }

    @Test
    @DisplayName("Debe lanzar ExternalServiceUnavailableException cuando ms-tiendas falla con error 500")
    void shouldThrowExternalServiceUnavailableOnServerError() {
        mockServer.expect(requestTo(BASE_URL + "/api/v1/admin/fundaciones/3"))
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
}
