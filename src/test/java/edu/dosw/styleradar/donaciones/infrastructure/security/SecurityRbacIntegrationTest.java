package edu.dosw.styleradar.donaciones.infrastructure.security;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.dosw.styleradar.donaciones.application.dto.request.CreatePublicationRequestDTO;
import edu.dosw.styleradar.donaciones.application.dto.request.DecideDonationRequestDTO;
import edu.dosw.styleradar.donaciones.application.dto.request.SchedulePickupRequestDTO;
import edu.dosw.styleradar.donaciones.domain.model.ConservationStatus;
import edu.dosw.styleradar.donaciones.domain.model.DeliveryMode;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequestStatus;
import edu.dosw.styleradar.donaciones.domain.model.FoundationSummary;
import edu.dosw.styleradar.donaciones.domain.model.Garment;
import edu.dosw.styleradar.donaciones.domain.model.GarmentType;
import edu.dosw.styleradar.donaciones.domain.model.PublicationStatus;
import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;
import edu.dosw.styleradar.donaciones.domain.model.Size;
import edu.dosw.styleradar.donaciones.domain.model.Style;
import edu.dosw.styleradar.donaciones.domain.port.in.ClaimDonationUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.ConfirmDonationReceiptUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.CreateDonationRequestUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.CreatePublicationUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.DecideDonationRequestUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.GetPartnerFoundationsUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.GetPublicationsUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.ManagePublicationUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.ScheduleDonationPickupUseCase;
import edu.dosw.styleradar.donaciones.domain.port.out.FoundationCatalogPort;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({SecondHandPublicationController.class, DonationRequestController.class})
@ActiveProfiles("test")
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtTokenProvider.class,
        SecurityErrorHandler.class,
        ResourceOwnershipValidator.class,
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
    private ClaimDonationUseCase claimDonationUseCase;

    @MockBean
    private DecideDonationRequestUseCase decideDonationRequestUseCase;

    @MockBean
    private ScheduleDonationPickupUseCase scheduleDonationPickupUseCase;

    @MockBean
    private ConfirmDonationReceiptUseCase confirmDonationReceiptUseCase;

    @MockBean
    private GetPartnerFoundationsUseCase getPartnerFoundationsUseCase;

    @MockBean
    private FoundationCatalogPort foundationCatalogPort;

    @Test
    @DisplayName("Debe retornar 401 Unauthorized (RFC 7807) cuando un cliente anónimo intenta crear una donación sin Bearer JWT")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/donations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("No autorizado"))
                .andExpect(jsonPath("$.type").value("https://api.styleradar.com/errors/unauthorized"))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    @DisplayName("Debe retornar 403 Forbidden (RFC 7807) cuando un USUARIO NORMAL intenta decidir sobre una donación de fundación")
    @WithMockUser(roles = "USER")
    void shouldReturn403WhenUserTriesToDecideDonation() throws Exception {
        DecideDonationRequestDTO request = new DecideDonationRequestDTO(501L, true, null);

        mockMvc.perform(patch("/api/v1/donations/1/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Acceso denegado"))
                .andExpect(jsonPath("$.type").value("https://api.styleradar.com/errors/forbidden"))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    @DisplayName("Debe retornar 403 Forbidden (RFC 7807) cuando una FUNDACIÓN intenta crear una publicación de venta de ropa")
    @WithMockUser(roles = "FOUNDATION")
    void shouldReturn403WhenFoundationTriesToCreatePublication() throws Exception {
        mockMvc.perform(post("/api/v1/second-hand/publications")
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

        mockMvc.perform(patch("/api/v1/donations/1/decision")
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

    @Test
    @DisplayName("Debe rechazar con 403 Forbidden cuando un usuario intenta crear una publicación a nombre de otro ID de usuario")
    void shouldDenyPublicationCreationWhenUserIdDoesNotMatchToken() throws Exception {
        String token = jwtTokenProvider.generateToken(101L, "user1@styleradar.com", Role.USER, "Usuario Uno");

        CreatePublicationRequestDTO request = new CreatePublicationRequestDTO(
                999L, // Diferente al ID 101 del token
                "Camisa Oxford",
                "Camisa blanca casi nueva",
                GarmentType.UPPER,
                "Zara",
                "Blanco",
                Style.CASUAL,
                Size.M,
                ConservationStatus.LIKE_NEW,
                new BigDecimal("50000.00"),
                List.of("https://styleradar.com/img1.jpg")
        );

        mockMvc.perform(post("/api/v1/second-hand/publications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Acceso denegado"))
                .andExpect(jsonPath("$.type").value("https://api.styleradar.com/errors/forbidden"));
    }

    @Test
    @DisplayName("Debe permitir crear publicación cuando el userId coincide con el token JWT del usuario")
    void shouldAllowPublicationCreationWhenUserIdMatchesToken() throws Exception {
        String token = jwtTokenProvider.generateToken(101L, "user1@styleradar.com", Role.USER, "Usuario Uno");

        CreatePublicationRequestDTO request = new CreatePublicationRequestDTO(
                101L, // Mismo ID del token
                "Camisa Oxford",
                "Camisa blanca casi nueva",
                GarmentType.UPPER,
                "Zara",
                "Blanco",
                Style.CASUAL,
                Size.M,
                ConservationStatus.LIKE_NEW,
                new BigDecimal("50000.00"),
                List.of("https://styleradar.com/img1.jpg")
        );

        Garment garment = Garment.builder()
                .name("Camisa Oxford")
                .type(GarmentType.UPPER)
                .brand("Zara")
                .color("Blanco")
                .style(Style.CASUAL)
                .build();

        SecondHandPublication created = SecondHandPublication.builder()
                .id(10L)
                .userId(101L)
                .garment(garment)
                .size(Size.M)
                .conservationStatus(ConservationStatus.LIKE_NEW)
                .price(new BigDecimal("50000.00"))
                .status(PublicationStatus.AVAILABLE)
                .photos(List.of("https://styleradar.com/img1.jpg"))
                .publishedAt(LocalDateTime.now())
                .build();

        when(createPublicationUseCase.create(any())).thenReturn(created);

        mockMvc.perform(post("/api/v1/second-hand/publications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.userId").value(101));
    }

    @Test
    @DisplayName("Debe rechazar con 403 Forbidden cuando una fundación intenta decidir sobre solicitud con foundationId ajeno")
    void shouldDenyDonationDecisionWhenFoundationIdDoesNotMatchToken() throws Exception {
        String token = jwtTokenProvider.generateToken(501L, "fundacion1@styleradar.com", Role.FOUNDATION, "Fundación Uno");

        DecideDonationRequestDTO request = new DecideDonationRequestDTO(999L, true, null);

        mockMvc.perform(patch("/api/v1/donations/1/decision")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Acceso denegado"));
    }

    @Test
    @DisplayName("Debe permitir al ADMIN operar sobre recursos de cualquier usuario o fundación")
    void shouldAllowAdminToOperateOnAnyResource() throws Exception {
        String token = jwtTokenProvider.generateToken(1L, "admin@styleradar.com", Role.ADMIN, "Admin");

        DecideDonationRequestDTO request = new DecideDonationRequestDTO(999L, true, null);

        DonationRequest accepted = DonationRequest.builder()
                .id(1L)
                .publicationId(10L)
                .donorId(101L)
                .foundationId(999L)
                .status(DonationRequestStatus.APPROVED)
                .requestedAt(LocalDateTime.now())
                .decidedAt(LocalDateTime.now())
                .build();

        when(decideDonationRequestUseCase.decide(any(), any(), anyBoolean(), any())).thenReturn(accepted);

        mockMvc.perform(patch("/api/v1/donations/1/decision")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("Debe retornar 403 Forbidden (BOLA/IDOR) cuando un tercero intenta consultar una solicitud ajena por ID")
    void shouldDenyDonationRequestByIdForThirdParty() throws Exception {
        String thirdPartyToken = jwtTokenProvider.generateToken(999L, "intruso@styleradar.com", Role.USER, "Tercero");

        DonationRequest foreignRequest = DonationRequest.builder()
                .id(1L)
                .publicationId(10L)
                .donorId(101L)
                .foundationId(501L)
                .status(DonationRequestStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        when(createDonationRequestUseCase.getById(1L)).thenReturn(foreignRequest);

        mockMvc.perform(get("/api/v1/donations/1")
                        .header("Authorization", "Bearer " + thirdPartyToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Acceso denegado"));
    }

    @Test
    @DisplayName("Debe permitir a una FUNDACIÓN participante agendar la recogida con su propio foundationId")
    void shouldAllowFoundationToSchedulePickup() throws Exception {
        String foundationToken = jwtTokenProvider.generateToken(501L, "fundacion@styleradar.com", Role.FOUNDATION, "Fundación");
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);
        SchedulePickupRequestDTO request = new SchedulePickupRequestDTO(501L, "Sede Chapinero Cra 7", start, end);

        DonationRequest inPickup = DonationRequest.builder()
                .id(1L)
                .publicationId(10L)
                .donorId(101L)
                .foundationId(501L)
                .status(DonationRequestStatus.IN_PICKUP)
                .deliveryPoint("Sede Chapinero Cra 7")
                .pickupStartTime(start)
                .pickupEndTime(end)
                .requestedAt(LocalDateTime.now())
                .build();

        when(scheduleDonationPickupUseCase.schedulePickup(any())).thenReturn(inPickup);

        mockMvc.perform(patch("/api/v1/donations/1/pickup")
                        .header("Authorization", "Bearer " + foundationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PICKUP"));
    }

    @Test
    @DisplayName("Debe permitir a una TIENDA (ROLE_STORE) marcar su publicación de segunda mano como vendida")
    void shouldAllowStoreRoleToMarkPublicationAsSold() throws Exception {
        String storeToken = jwtTokenProvider.generateToken(202L, "tienda@styleradar.com", Role.STORE, "Tienda Eco");

        SecondHandPublication sold = SecondHandPublication.builder()
                .id(10L)
                .userId(202L)
                .garment(Garment.builder().name("Chaqueta").type(GarmentType.UPPER).brand("Levi's").color("Azul").style(Style.URBAN).build())
                .size(Size.M)
                .conservationStatus(ConservationStatus.LIKE_NEW)
                .price(new BigDecimal("60000.00"))
                .status(PublicationStatus.SOLD)
                .photos(List.of("https://styleradar.com/img1.jpg"))
                .publishedAt(LocalDateTime.now())
                .build();

        when(managePublicationUseCase.markAsSold(202L, 10L, 303L)).thenReturn(sold);

        mockMvc.perform(patch("/api/v1/second-hand/publications/10/sale")
                        .header("Authorization", "Bearer " + storeToken)
                        .param("userId", "202")
                        .param("compradorId", "303"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLD"));
    }

    @Test
    @DisplayName("Debe retornar 401 sin token en GET /donaciones/foundations y 200 con token USER")
    void shouldEnforceAuthOnPartnerFoundationsEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/donations/foundations"))
                .andExpect(status().isUnauthorized());

        String userToken = jwtTokenProvider.generateToken(101L, "user@styleradar.com", Role.USER, "Comprador");
        when(getPartnerFoundationsUseCase.getActiveFoundations(null)).thenReturn(List.of(
                new FoundationSummary(3L, "Fundación Abrigo Solidario", "Chapinero", "Desc", "Dir", "Tel",
                        "contacto@abrigosolidario.org", List.of("SUPERIOR"), "Niñez", "08:00-17:00", "Política", true)
        ));

        mockMvc.perform(get("/api/v1/donations/foundations")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3L));
    }

    @Test
    @DisplayName("Debe permitir al representante autorizado en ms-tiendas decidir sobre su fundación y rechazar coincidencia numérica sin representación")
    void shouldEnforceFoundationRepresentationValidationStrictly() throws Exception {
        // Caso positivo: usuario 24 es representanteId de la fundación 3 según ms-tiendas
        String authorizedRepToken = jwtTokenProvider.generateToken(24L, "rep24@styleradar.com", Role.FOUNDATION, "Representante");
        when(foundationCatalogPort.isFoundationRepresentative(24L, 3L)).thenReturn(true);

        DonationRequest accepted = DonationRequest.builder()
                .id(1L)
                .publicationId(10L)
                .donorId(101L)
                .foundationId(3L)
                .status(DonationRequestStatus.APPROVED)
                .requestedAt(LocalDateTime.now())
                .decidedAt(LocalDateTime.now())
                .build();
        when(decideDonationRequestUseCase.decide(3L, 1L, true, null)).thenReturn(accepted);

        DecideDonationRequestDTO validRequest = new DecideDonationRequestDTO(3L, true, null);
        mockMvc.perform(patch("/api/v1/donations/1/decision")
                        .header("Authorization", "Bearer " + authorizedRepToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // Caso negativo: usuario 3 intenta operar sobre fundación 3 por coincidencia numérica sin ser representanteId
        String collidingUserToken = jwtTokenProvider.generateToken(3L, "intruso3@styleradar.com", Role.FOUNDATION, "Intruso");
        when(foundationCatalogPort.isFoundationRepresentative(3L, 3L)).thenReturn(false);

        mockMvc.perform(patch("/api/v1/donations/1/decision")
                        .header("Authorization", "Bearer " + collidingUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/api/v1/donations/foundations/3/requests")
                        .header("Authorization", "Bearer " + collidingUserToken))
                .andExpect(status().isForbidden());

        DonationRequest foreignRequest = DonationRequest.builder()
                .id(1L)
                .publicationId(10L)
                .donorId(101L)
                .foundationId(3L)
                .status(DonationRequestStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();
        when(createDonationRequestUseCase.getById(1L)).thenReturn(foreignRequest);

        mockMvc.perform(get("/api/v1/donations/1")
                        .header("Authorization", "Bearer " + collidingUserToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Debe aceptar tokens oficiales de ms-auth (sub numérico + lista roles) y permitir preflight CORS")
    void shouldAcceptOfficialMsAuthJwtAndCorsPreflight() throws Exception {
        String testSecret = "c3R5bGVyYWRhci1zZWNyZXQta2V5LWZvci1kb3N3LTIwMjYtcHJvamVjdC1qd3Q=";
        javax.crypto.SecretKey key = new javax.crypto.spec.SecretKeySpec(
                java.util.Base64.getDecoder().decode(testSecret), "HmacSHA256");
        java.time.Instant now = java.time.Instant.now();

        String msAuthFoundationJwt = io.jsonwebtoken.Jwts.builder()
                .subject("24")
                .claim("roles", List.of("REPRESENTANTE_FUNDACION"))
                .claim("iat", now.getEpochSecond())
                .claim("exp", now.plusSeconds(3600).getEpochSecond())
                .signWith(key, io.jsonwebtoken.Jwts.SIG.HS256)
                .compact();

        FoundationSummary foundation = new FoundationSummary(
                3L, "Fundación Abrigo Solidario", "Chapinero", "Desc", "Dir", "Tel",
                "contacto@abrigosolidario.org", List.of("SUPERIOR"), "Niñez", "08:00-17:00", "Política", true, 24L
        );
        when(getPartnerFoundationsUseCase.getFoundationByRepresentativeId(24L)).thenReturn(foundation);

        mockMvc.perform(get("/api/v1/donations/foundations/representative")
                        .header("Authorization", "Bearer " + msAuthFoundationJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3L))
                .andExpect(jsonPath("$.representanteId").value(24L));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/v1/donations/foundations")
                        .header("Origin", "https://styleradar-front.azurewebsites.net")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Debe ignorar donorId enviado en el payload de POST /api/v1/donations y tomar siempre el del token JWT")
    void shouldIgnoreSpoofedDonorIdInPayloadAndResolveFromJwt() throws Exception {
        String userToken = jwtTokenProvider.generateToken(101L, "user1@styleradar.com", Role.USER, "Donante Real");
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);

        String payloadWithSpoofedDonor = """
                {
                  "publicationId": 10,
                  "donorId": 999,
                  "foundationId": 3,
                  "message": "Donación desde sesión real",
                  "deliveryProposal": {
                    "deliveryMode": "DROP_OFF_POINT",
                    "deliveryPoint": "Calle 85 # 15-32, Chapinero, Bogotá",
                    "startTime": "%s",
                    "endTime": "%s"
                  }
                }
                """.formatted(start, end);

        DonationRequest created = DonationRequest.builder()
                .id(1L)
                .publicationId(10L)
                .donorId(101L)
                .foundationId(3L)
                .status(DonationRequestStatus.PENDING)
                .deliveryMode(DeliveryMode.DROP_OFF_POINT)
                .deliveryPoint("Calle 85 # 15-32, Chapinero, Bogotá")
                .pickupStartTime(start)
                .pickupEndTime(end)
                .requestedAt(LocalDateTime.now())
                .build();

        when(createDonationRequestUseCase.create(org.mockito.ArgumentMatchers.argThat(cmd ->
                cmd != null && Long.valueOf(101L).equals(cmd.donorId())
        ))).thenReturn(created);

        mockMvc.perform(post("/api/v1/donations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadWithSpoofedDonor))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.donorId").value(101L));
    }

    @Test
    @DisplayName("BUG-003 JWT real: GET /api/v1/donations sin token retorna 401 Unauthorized")
    void shouldRejectListDonationsWithoutJwtToken() throws Exception {
        mockMvc.perform(get("/api/v1/donations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("BUG-003 JWT real: GET /api/v1/donations para donante USER retorna su propio historial")
    void shouldListDonationsForRealUserJwt() throws Exception {
        String userToken = jwtTokenProvider.generateToken(101L, "donor@styleradar.com", Role.USER, "Donante");
        DonationRequest pending = buildSampleDonation(10L, 101L, 3L, DonationRequestStatus.PENDING);
        DonationRequest approved = buildSampleDonation(11L, 101L, 3L, DonationRequestStatus.APPROVED);
        when(createDonationRequestUseCase.getByDonorId(101L)).thenReturn(List.of(pending, approved));

        mockMvc.perform(get("/api/v1/donations")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].donorId").value(101L));

        mockMvc.perform(get("/api/v1/donations")
                        .param("status", "APPROVED")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("APPROVED"));
    }

    @Test
    @DisplayName("BUG-003 JWT real: GET /api/v1/donations para tienda STORE retorna sus propias donaciones")
    void shouldListDonationsForRealStoreJwt() throws Exception {
        String storeToken = jwtTokenProvider.generateToken(301L, "store@styleradar.com", Role.STORE, "Tienda Centro");
        DonationRequest storeDonation = buildSampleDonation(20L, 301L, 3L, DonationRequestStatus.PENDING);
        when(createDonationRequestUseCase.getByDonorId(301L)).thenReturn(List.of(storeDonation));

        mockMvc.perform(get("/api/v1/donations")
                        .header("Authorization", "Bearer " + storeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].donorId").value(301L));
    }

    @Test
    @DisplayName("BUG-003 JWT real: GET /api/v1/donations para FOUNDATION resuelve fundación verificada y retorna bandeja")
    void shouldListDonationsForRealFoundationJwt() throws Exception {
        String foundationToken = jwtTokenProvider.generateToken(24L, "rep@abrigosolidario.org", Role.FOUNDATION, "Representante");
        FoundationSummary foundation = new FoundationSummary(
                3L, "Fundación Abrigo Solidario", "Chapinero", "Recolección", "Calle 85",
                "3001234567", "f@org.co", List.of("SUPERIOR"), "Niñez", "08-17", "Limpias", true, 24L
        );
        DonationRequest inboxItem = buildSampleDonation(30L, 101L, 3L, DonationRequestStatus.PENDING);

        when(getPartnerFoundationsUseCase.getFoundationByRepresentativeId(24L)).thenReturn(foundation);
        when(createDonationRequestUseCase.getByFoundationId(3L, null)).thenReturn(List.of(inboxItem));

        mockMvc.perform(get("/api/v1/donations")
                        .header("Authorization", "Bearer " + foundationToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].foundationId").value(3L));
    }

    @Test
    @DisplayName("BUG-003 JWT real Opción B1: GET /api/v1/donations para ADMIN retorna todas las donaciones globales")
    void shouldListAllDonationsForRealAdminJwt() throws Exception {
        String adminToken = jwtTokenProvider.generateToken(1L, "admin@styleradar.com", Role.ADMIN, "Admin");
        DonationRequest d1 = buildSampleDonation(40L, 101L, 3L, DonationRequestStatus.PENDING);
        DonationRequest d2 = buildSampleDonation(41L, 301L, 5L, DonationRequestStatus.APPROVED);

        when(createDonationRequestUseCase.getAll(null)).thenReturn(List.of(d1, d2));
        when(createDonationRequestUseCase.getAll(DonationRequestStatus.APPROVED)).thenReturn(List.of(d2));

        mockMvc.perform(get("/api/v1/donations")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/v1/donations")
                        .param("status", "APPROVED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(41L));
    }

    @Test
    @DisplayName("BUG-003 JWT real: debe rechazar suplantación entre actores y propagar 404/503 de ms-tiendas")
    void shouldRejectImpersonationAndHandleTiendasOutcomesWithRealJwt() throws Exception {
        String userToken = jwtTokenProvider.generateToken(101L, "donor@styleradar.com", Role.USER, "Donante");
        String intruderFoundationToken = jwtTokenProvider.generateToken(999L, "intruder@org.co", Role.FOUNDATION, "Intruso");

        // Suplantación de donante por otro USER -> 403
        mockMvc.perform(get("/api/v1/donations/donors/202")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        // Suplantación de fundación en endpoint por fundación -> 403 cuando no es representante
        when(foundationCatalogPort.isFoundationRepresentative(999L, 3L)).thenReturn(false);
        mockMvc.perform(get("/api/v1/donations/foundations/3/requests")
                        .header("Authorization", "Bearer " + intruderFoundationToken))
                .andExpect(status().isForbidden());

        // FOUNDATION sin asociación verificable en ms-tiendas en GET /api/v1/donations -> 404
        when(getPartnerFoundationsUseCase.getFoundationByRepresentativeId(999L))
                .thenThrow(new edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException(
                        "No se encontró fundación asociada"));
        mockMvc.perform(get("/api/v1/donations")
                        .header("Authorization", "Bearer " + intruderFoundationToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        // Fallo de ms-tiendas al resolver FOUNDATION en GET /api/v1/donations -> 503
        doThrow(new edu.dosw.styleradar.donaciones.domain.exception.ExternalServiceUnavailableException(
                        "ms-tiendas no disponible"))
                .when(getPartnerFoundationsUseCase).getFoundationByRepresentativeId(999L);
        mockMvc.perform(get("/api/v1/donations")
                        .header("Authorization", "Bearer " + intruderFoundationToken))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    @DisplayName("GET /api/v1/donations/{id}: con ms-tiendas indisponible el donante propietario consulta su donación (200) y donante ajeno recibe 403 sin llamar a Tiendas")
    void shouldAllowOwnerDonorAndDenyForeignDonorOnGetByIdWhenTiendasUnavailable() throws Exception {
        String ownerToken = jwtTokenProvider.generateToken(101L, "donor@styleradar.com", Role.USER, "Donante Propietario");
        String foreignDonorToken = jwtTokenProvider.generateToken(202L, "foreign@styleradar.com", Role.USER, "Donante Ajeno");

        DonationRequest donation = buildSampleDonation(55L, 101L, 3L, DonationRequestStatus.PENDING);
        when(createDonationRequestUseCase.getById(55L)).thenReturn(donation);
        doThrow(new edu.dosw.styleradar.donaciones.domain.exception.ExternalServiceUnavailableException(
                        "ms-tiendas no disponible"))
                .when(foundationCatalogPort).isFoundationRepresentative(any(), any());

        mockMvc.perform(get("/api/v1/donations/55")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(55L))
                .andExpect(jsonPath("$.donorId").value(101L));

        mockMvc.perform(get("/api/v1/donations/55")
                        .header("Authorization", "Bearer " + foreignDonorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verify(foundationCatalogPort, never()).isFoundationRepresentative(any(), any());
    }

    @Test
    @DisplayName("BUG-003 JWT real: GET /api/v1/donations debe ignorar cabeceras de identidad falsificadas con o sin token")
    void shouldIgnoreSpoofedIdentityHeadersOnListDonationsEndpoint() throws Exception {
        // 1. Sin JWT real pero con cabeceras de identidad falsificadas -> 401 Unauthorized
        mockMvc.perform(get("/api/v1/donations")
                        .header("X-User-Id", "1")
                        .header("X-Authenticated-User-Id", "1")
                        .header("X-User-Roles", "ROLE_ADMIN")
                        .header("X-Roles", "ADMIN")
                        .header("X-Donor-Id", "101")
                        .header("X-Foundation-Id", "3"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        // 2. Con JWT real USER (101L) y cabeceras falsificadas de ADMIN/FOUNDATION/otro usuario -> usa solo el JWT (101L)
        String userToken = jwtTokenProvider.generateToken(101L, "donor@styleradar.com", Role.USER, "Donante");
        DonationRequest ownDonation = buildSampleDonation(70L, 101L, 3L, DonationRequestStatus.PENDING);
        when(createDonationRequestUseCase.getByDonorId(101L)).thenReturn(List.of(ownDonation));

        mockMvc.perform(get("/api/v1/donations")
                        .header("Authorization", "Bearer " + userToken)
                        .header("X-User-Id", "999")
                        .header("X-Authenticated-User-Id", "1")
                        .header("X-User-Roles", "ROLE_ADMIN,ROLE_FOUNDATION")
                        .header("X-Roles", "ROLE_ADMIN")
                        .header("X-Donor-Id", "202")
                        .header("X-Foundation-Id", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].donorId").value(101L));

        verify(createDonationRequestUseCase, never()).getAll(any());
        verify(getPartnerFoundationsUseCase, never()).getFoundationByRepresentativeId(any());
    }

    private DonationRequest buildSampleDonation(Long id, Long donorId, Long foundationId, DonationRequestStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return DonationRequest.builder()
                .id(id)
                .publicationId(10L)
                .donorId(donorId)
                .foundationId(foundationId)
                .status(status)
                .deliveryMode(DeliveryMode.DROP_OFF_POINT)
                .deliveryPoint("Calle 85 # 15-32, Chapinero, Bogotá")
                .pickupStartTime(now.plusDays(1))
                .pickupEndTime(now.plusDays(1).plusHours(2))
                .requestedAt(now)
                .build();
    }
}
