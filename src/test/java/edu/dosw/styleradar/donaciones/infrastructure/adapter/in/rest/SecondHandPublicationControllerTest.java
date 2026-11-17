package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.dosw.styleradar.donaciones.application.dto.request.CreatePublicationRequestDTO;
import edu.dosw.styleradar.donaciones.application.dto.request.EditPublicationRequestDTO;
import edu.dosw.styleradar.donaciones.domain.exception.DomainConflictException;
import edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException;
import edu.dosw.styleradar.donaciones.domain.model.ConservationStatus;
import edu.dosw.styleradar.donaciones.domain.model.Garment;
import edu.dosw.styleradar.donaciones.domain.model.GarmentType;
import edu.dosw.styleradar.donaciones.domain.model.PublicationStatus;
import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;
import edu.dosw.styleradar.donaciones.domain.model.Size;
import edu.dosw.styleradar.donaciones.domain.model.Style;
import edu.dosw.styleradar.donaciones.domain.port.in.CreatePublicationUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.GetPublicationsUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.ManagePublicationUseCase;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest.mapper.PublicationRestMapper;
import edu.dosw.styleradar.donaciones.infrastructure.security.JwtAuthenticationFilter;
import edu.dosw.styleradar.donaciones.infrastructure.security.JwtTokenProvider;
import edu.dosw.styleradar.donaciones.infrastructure.security.ResourceOwnershipValidator;
import edu.dosw.styleradar.donaciones.infrastructure.security.SecurityConfig;
import edu.dosw.styleradar.donaciones.infrastructure.security.SecurityErrorHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SecondHandPublicationController.class)
@ActiveProfiles("test")
@Import({
        PublicationRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtTokenProvider.class,
        SecurityErrorHandler.class
})
@WithMockUser(roles = "USER")
class SecondHandPublicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreatePublicationUseCase createPublicationUseCase;

    @MockBean
    private GetPublicationsUseCase getPublicationsUseCase;

    @MockBean
    private ManagePublicationUseCase managePublicationUseCase;

    @MockBean
    private ResourceOwnershipValidator resourceOwnershipValidator;

    @Autowired
    private PublicationRestMapper publicationRestMapper;

    private SecondHandPublication buildSamplePublication(Long id, Long userId) {
        Garment garment = Garment.builder()
                .name("Chaqueta Denim")
                .description("Estilo 90s")
                .type(GarmentType.UPPER)
                .brand("Levi's")
                .color("Azul")
                .style(Style.URBAN)
                .build();

        return SecondHandPublication.builder()
                .id(id)
                .userId(userId)
                .garment(garment)
                .size(Size.M)
                .conservationStatus(ConservationStatus.LIKE_NEW)
                .price(new BigDecimal("75000.00"))
                .status(PublicationStatus.AVAILABLE)
                .publishedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .photos(List.of("https://cdn.test.com/foto1.jpg"))
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/second-hand/publications - Debe crear una publicación y responder 201 Created")
    void shouldCreatePublicationSuccessfully() throws Exception {
        CreatePublicationRequestDTO request = new CreatePublicationRequestDTO(
                101L, "Chaqueta Denim", "Estilo 90s", GarmentType.UPPER, "Levi's", "Azul",
                Style.URBAN, Size.M, ConservationStatus.LIKE_NEW, new BigDecimal("75000.00"),
                List.of("https://cdn.test.com/foto1.jpg")
        );

        SecondHandPublication created = buildSamplePublication(1L, 101L);
        when(createPublicationUseCase.create(any())).thenReturn(created);

        mockMvc.perform(post("/api/v1/second-hand/publications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/second-hand/publications/1"))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.garmentName").value("Chaqueta Denim"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("POST /api/v1/second-hand/publications - Debe responder 400 Bad Request cuando faltan datos obligatorios")
    void shouldFailCreationWhenInvalidData() throws Exception {
        CreatePublicationRequestDTO invalidRequest = new CreatePublicationRequestDTO(
                null, "", null, null, "", "", null, null, null, null, null
        );

        mockMvc.perform(post("/api/v1/second-hand/publications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Error de validación"))
                .andExpect(jsonPath("$.invalidParams").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/second-hand/publications/{id} - Debe responder 200 OK cuando existe la publicación")
    void shouldGetPublicationById() throws Exception {
        SecondHandPublication publication = buildSamplePublication(1L, 101L);
        when(getPublicationsUseCase.getById(1L)).thenReturn(publication);

        mockMvc.perform(get("/api/v1/second-hand/publications/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.garmentName").value("Chaqueta Denim"));
    }

    @Test
    @DisplayName("GET /api/v1/second-hand/publications/{id} - Debe responder 404 Not Found cuando no existe")
    void shouldReturn404WhenPublicationNotFound() throws Exception {
        when(getPublicationsUseCase.getById(999L)).thenThrow(new ResourceNotFoundException("No existe publicación con ID 999"));

        mockMvc.perform(get("/api/v1/second-hand/publications/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.detail").value("No existe publicación con ID 999"));
    }

    @Test
    @DisplayName("GET /api/v1/second-hand/publications/users/{userId} - Debe responder 200 OK con lista de publicaciones")
    void shouldGetPublicationsByUserId() throws Exception {
        SecondHandPublication publication = buildSamplePublication(1L, 101L);
        when(getPublicationsUseCase.getByUserId(101L)).thenReturn(List.of(publication));

        mockMvc.perform(get("/api/v1/second-hand/publications/users/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].userId").value(101L));
    }

    @Test
    @DisplayName("GET /api/v1/second-hand/publications - Debe buscar con filtros y responder 200 OK")
    void shouldSearchPublicationsWithFilters() throws Exception {
        SecondHandPublication publication = buildSamplePublication(1L, 101L);
        when(getPublicationsUseCase.search(any())).thenReturn(List.of(publication));

        mockMvc.perform(get("/api/v1/second-hand/publications")
                        .param("brand", "Levi's")
                        .param("garmentType", "UPPER")
                        .param("status", "AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].brand").value("Levi's"));

        verify(getPublicationsUseCase).search(argThat(criteria ->
                criteria.getType() == GarmentType.UPPER && "Levi's".equals(criteria.getBrand())));
    }

    @Test
    @DisplayName("PUT /api/v1/second-hand/publications/{id} - Debe editar y responder 200 OK")
    void shouldEditPublication() throws Exception {
        EditPublicationRequestDTO request = new EditPublicationRequestDTO(
                101L, "Chaqueta Modificada", "Nueva desc", GarmentType.UPPER, "Levi's", "Azul Oscuro",
                Style.URBAN, Size.L, ConservationStatus.GOOD_CONDITION, new BigDecimal("80000.00"),
                List.of("https://cdn.test.com/foto2.jpg")
        );

        SecondHandPublication updated = buildSamplePublication(1L, 101L);
        updated.getGarment().setName("Chaqueta Modificada");
        when(managePublicationUseCase.edit(any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/second-hand/publications/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.garmentName").value("Chaqueta Modificada"));
    }

    @Test
    @DisplayName("PATCH /api/v1/second-hand/publications/{id}/sale - Debe marcar como vendida con compradorId obligatorio y responder 200 OK")
    void shouldMarkAsSold() throws Exception {
        SecondHandPublication sold = buildSamplePublication(1L, 101L);
        sold.setStatus(PublicationStatus.SOLD);
        when(managePublicationUseCase.markAsSold(101L, 1L, 102L)).thenReturn(sold);

        mockMvc.perform(patch("/api/v1/second-hand/publications/1/sale")
                        .param("userId", "101")
                        .param("compradorId", "102"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLD"));
    }

    @Test
    @DisplayName("PATCH /api/v1/second-hand/publications/{id}/sale - Debe responder 400 Bad Request cuando falta compradorId")
    void shouldFailWhenCompradorIdIsMissing() throws Exception {
        mockMvc.perform(patch("/api/v1/second-hand/publications/1/sale")
                        .param("userId", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/v1/second-hand/publications/{id}/sale - Debe responder 409 Conflict si el usuario no es el propietario")
    void shouldReturnConflictWhenUserIsNotOwner() throws Exception {
        when(managePublicationUseCase.markAsSold(999L, 1L, 102L))
                .thenThrow(new DomainConflictException("Solo el propietario puede modificar el estado de la publicación"));

        mockMvc.perform(patch("/api/v1/second-hand/publications/1/sale")
                        .param("userId", "999")
                        .param("compradorId", "102"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflicto de dominio"));
    }

    @Test
    @DisplayName("DELETE /api/v1/second-hand/publications/{id} - Debe retirar publicación y responder 204 No Content")
    void shouldRetirePublication() throws Exception {
        doNothing().when(managePublicationUseCase).retire(101L, 1L);

        mockMvc.perform(delete("/api/v1/second-hand/publications/1")
                        .param("userId", "101"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PATCH /api/v1/second-hand/publications/{id}/donation - Debe destinar a donación y responder 200 OK")
    void shouldDonatePublication() throws Exception {
        SecondHandPublication inDonation = buildSamplePublication(1L, 101L);
        inDonation.setStatus(PublicationStatus.IN_DONATION);
        when(managePublicationUseCase.markForDonation(101L, 1L)).thenReturn(inDonation);

        mockMvc.perform(patch("/api/v1/second-hand/publications/1/donation")
                        .param("userId", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_DONATION"));
    }

    @Test
    @DisplayName("DELETE /api/v1/second-hand/publications/{id} - Debe retornar 403 Forbidden cuando el usuario no es el propietario ni admin")
    void shouldReturn403WhenUserIsNotOwner() throws Exception {
        doThrow(new AccessDeniedException("Acceso denegado: El usuario no es propietario"))
                .when(resourceOwnershipValidator).validateOwnerOrAdmin(999L);

        mockMvc.perform(delete("/api/v1/second-hand/publications/1")
                        .param("userId", "999"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Acceso denegado"))
                .andExpect(jsonPath("$.type").value("https://styleradar.dosw.edu/errors/forbidden"));
    }
}
