package edu.dosw.styleradar.donaciones.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.dosw.styleradar.donaciones.application.dto.request.DecideDonationRequestDTO;
import edu.dosw.styleradar.donaciones.domain.port.in.ConfirmDonationReceiptUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.CreateDonationRequestUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.CreatePublicationUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.DecideDonationRequestUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.GetPublicationsUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.ManagePublicationUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.ScheduleDonationPickupUseCase;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest.DonationRequestController;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest.GlobalExceptionHandler;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest.SecondHandPublicationController;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest.mapper.DonationRequestRestMapper;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest.mapper.PublicationRestMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({SecondHandPublicationController.class, DonationRequestController.class})
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtTokenProvider.class,
        SecurityErrorHandler.class,
        PublicationRestMapper.class,
        DonationRequestRestMapper.class,
        GlobalExceptionHandler.class
})
class SecurityRbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CreatePublicationUseCase createPublicationUseCase;

    @MockBean
    private GetPublicationsUseCase getPublicationsUseCase;

    @MockBean
    private ManagePublicationUseCase managePublicationUseCase;

    @MockBean
    private CreateDonationRequestUseCase createDonationRequestUseCase;

    @MockBean
    private DecideDonationRequestUseCase decideDonationRequestUseCase;

    @MockBean
    private ScheduleDonationPickupUseCase scheduleDonationPickupUseCase;

    @MockBean
    private ConfirmDonationReceiptUseCase confirmDonationReceiptUseCase;

    @Test
    @DisplayName("Debe retornar 401 Unauthorized (RFC 7807) cuando un cliente anónimo intenta crear una donación sin Bearer JWT")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/donaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("No autorizado"))
                .andExpect(jsonPath("$.type").value("https://styleradar.dosw.edu/errors/unauthorized"))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    @DisplayName("Debe retornar 403 Forbidden (RFC 7807) cuando un USUARIO NORMAL intenta decidir sobre una donación de fundación")
    @WithMockUser(roles = "USER")
    void shouldReturn403WhenUserTriesToDecideDonation() throws Exception {
        DecideDonationRequestDTO request = new DecideDonationRequestDTO(501L, true, null);

        mockMvc.perform(patch("/donaciones/1/decide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Acceso denegado"))
                .andExpect(jsonPath("$.type").value("https://styleradar.dosw.edu/errors/forbidden"))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    @DisplayName("Debe retornar 403 Forbidden (RFC 7807) cuando una FUNDACIÓN intenta crear una publicación de venta de ropa")
    @WithMockUser(roles = "FOUNDATION")
    void shouldReturn403WhenFoundationTriesToCreatePublication() throws Exception {
        mockMvc.perform(post("/segunda-mano")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Debe autenticar correctamente usando una cabecera Authorization con token Bearer JWT real emitido")
    void shouldAuthenticateWithRealJwtBearerHeader() throws Exception {
        String token = jwtTokenProvider.generateToken(1L, "admin@styleradar.com", Role.ADMIN, "Admin");

        // El endpoint /donaciones/1/decide acepta ADMIN; con token real debe superar el filtro de seguridad
        DecideDonationRequestDTO request = new DecideDonationRequestDTO(501L, true, null);

        mockMvc.perform(patch("/donaciones/1/decide")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Verificar métodos de UserPrincipal y Role para cobertura integral")
    void shouldVerifyUserPrincipalAndRole() {
        UserPrincipal principal = new UserPrincipal(1L, "admin@styleradar.com", "pass", "Admin", Role.ADMIN);

        assertThat(principal.id()).isEqualTo(1L);
        assertThat(principal.getUsername()).isEqualTo("admin@styleradar.com");
        assertThat(principal.getPassword()).isEqualTo("pass");
        assertThat(principal.role()).isEqualTo(Role.ADMIN);
        assertThat(principal.isAccountNonExpired()).isTrue();
        assertThat(principal.isAccountNonLocked()).isTrue();
        assertThat(principal.isCredentialsNonExpired()).isTrue();
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");

        assertThat(Role.USER.getAuthority()).isEqualTo("ROLE_USER");
        assertThat(Role.FOUNDATION.getAuthority()).isEqualTo("ROLE_FOUNDATION");
    }
}
