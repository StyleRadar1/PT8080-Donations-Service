package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest;

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

import edu.dosw.styleradar.donaciones.application.dto.request.ConfirmReceiptRequestDTO;
import edu.dosw.styleradar.donaciones.application.dto.request.CreateDonationRequestDTO;
import edu.dosw.styleradar.donaciones.application.dto.request.DecideDonationRequestDTO;
import edu.dosw.styleradar.donaciones.application.dto.request.SchedulePickupRequestDTO;
import edu.dosw.styleradar.donaciones.domain.exception.DomainConflictException;
import edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequestStatus;
import edu.dosw.styleradar.donaciones.domain.port.in.ConfirmDonationReceiptUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.CreateDonationRequestUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.DecideDonationRequestUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.ScheduleDonationPickupUseCase;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest.mapper.DonationRequestRestMapper;
import edu.dosw.styleradar.donaciones.infrastructure.security.JwtAuthenticationFilter;
import edu.dosw.styleradar.donaciones.infrastructure.security.JwtTokenProvider;
import edu.dosw.styleradar.donaciones.infrastructure.security.SecurityConfig;
import edu.dosw.styleradar.donaciones.infrastructure.security.SecurityErrorHandler;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DonationRequestController.class)
@Import({
        DonationRequestRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtTokenProvider.class,
        SecurityErrorHandler.class
})
@WithMockUser(roles = {"USER", "FOUNDATION"})
class DonationRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateDonationRequestUseCase createDonationRequestUseCase;

    @MockBean
    private DecideDonationRequestUseCase decideDonationRequestUseCase;

    @MockBean
    private ScheduleDonationPickupUseCase scheduleDonationPickupUseCase;

    @MockBean
    private ConfirmDonationReceiptUseCase confirmDonationReceiptUseCase;

    @Autowired
    private DonationRequestRestMapper donationRequestRestMapper;

    private DonationRequest buildSampleRequest(Long id, DonationRequestStatus status) {
        return DonationRequest.builder()
                .id(id)
                .publicationId(10L)
                .donorId(101L)
                .foundationId(501L)
                .message("Deseo donar")
                .status(status)
                .deliveryPoint("Cra 7 # 45")
                .requestedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /donaciones - Debe crear solicitud de donación y responder 201 Created")
    void shouldCreateDonationRequestSuccessfully() throws Exception {
        CreateDonationRequestDTO request = new CreateDonationRequestDTO(10L, 101L, 501L, "Deseo donar");
        DonationRequest created = buildSampleRequest(1L, DonationRequestStatus.PENDING);

        when(createDonationRequestUseCase.create(any())).thenReturn(created);

        mockMvc.perform(post("/donaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/donaciones/1"))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /donaciones - Debe responder 400 Bad Request cuando faltan IDs obligatorios")
    void shouldFailWhenMissingRequiredFields() throws Exception {
        CreateDonationRequestDTO request = new CreateDonationRequestDTO(null, null, null, null);

        mockMvc.perform(post("/donaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Error de validación"));
    }

    @Test
    @DisplayName("GET /donaciones/{id} - Debe responder 200 OK cuando existe la solicitud")
    void shouldGetDonationRequestById() throws Exception {
        DonationRequest request = buildSampleRequest(1L, DonationRequestStatus.PENDING);
        when(createDonationRequestUseCase.getById(1L)).thenReturn(request);

        mockMvc.perform(get("/donaciones/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.donorId").value(101L));
    }

    @Test
    @DisplayName("GET /donaciones/{id} - Debe responder 404 Not Found cuando no existe")
    void shouldReturn404WhenNotFound() throws Exception {
        when(createDonationRequestUseCase.getById(999L))
                .thenThrow(new ResourceNotFoundException("No existe solicitud con ID 999"));

        mockMvc.perform(get("/donaciones/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    @DisplayName("GET /donaciones/donor/{donorId} - Debe responder 200 OK con la lista de solicitudes del donante")
    void shouldGetRequestsByDonor() throws Exception {
        DonationRequest request = buildSampleRequest(1L, DonationRequestStatus.PENDING);
        when(createDonationRequestUseCase.getByDonorId(101L)).thenReturn(List.of(request));

        mockMvc.perform(get("/donaciones/donor/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].donorId").value(101L));
    }

    @Test
    @DisplayName("GET /donaciones/foundation/{foundationId} - Debe responder 200 OK con solicitudes de la fundación")
    void shouldGetRequestsByFoundation() throws Exception {
        DonationRequest request = buildSampleRequest(1L, DonationRequestStatus.PENDING);
        when(createDonationRequestUseCase.getByFoundationId(eq(501L), any())).thenReturn(List.of(request));

        mockMvc.perform(get("/donaciones/foundation/501")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].foundationId").value(501L));
    }

    @Test
    @DisplayName("PATCH /donaciones/{id}/decide - Debe aprobar solicitud y responder 200 OK")
    void shouldDecideDonationRequest() throws Exception {
        DecideDonationRequestDTO request = new DecideDonationRequestDTO(501L, true, null);
        DonationRequest approved = buildSampleRequest(1L, DonationRequestStatus.APPROVED);

        when(decideDonationRequestUseCase.decide(eq(501L), eq(1L), eq(true), any())).thenReturn(approved);

        mockMvc.perform(patch("/donaciones/1/decide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("PATCH /donaciones/{id}/decide - Debe responder 409 Conflict si la fundación no corresponde")
    void shouldReturnConflictOnDecisionError() throws Exception {
        DecideDonationRequestDTO request = new DecideDonationRequestDTO(999L, true, null);

        when(decideDonationRequestUseCase.decide(eq(999L), eq(1L), anyBoolean(), any()))
                .thenThrow(new DomainConflictException("La solicitud no está dirigida a esta fundación"));

        mockMvc.perform(patch("/donaciones/1/decide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflicto de dominio"));
    }

    @Test
    @DisplayName("PATCH /donaciones/{id}/pickup - Debe agendar recogida y responder 200 OK")
    void shouldSchedulePickup() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        LocalDateTime end = start.plusHours(3);
        SchedulePickupRequestDTO request = new SchedulePickupRequestDTO(101L, "Cra 15 # 85", start, end);

        DonationRequest scheduled = buildSampleRequest(1L, DonationRequestStatus.IN_PICKUP);
        when(scheduleDonationPickupUseCase.schedulePickup(any())).thenReturn(scheduled);

        mockMvc.perform(patch("/donaciones/1/pickup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PICKUP"));
    }

    @Test
    @DisplayName("PATCH /donaciones/{id}/confirm - Debe confirmar recepción y responder 200 OK")
    void shouldConfirmReceipt() throws Exception {
        ConfirmReceiptRequestDTO request = new ConfirmReceiptRequestDTO(501L);
        DonationRequest delivered = buildSampleRequest(1L, DonationRequestStatus.DELIVERED);

        when(confirmDonationReceiptUseCase.confirmReceipt(501L, 1L)).thenReturn(delivered);

        mockMvc.perform(patch("/donaciones/1/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }
}
