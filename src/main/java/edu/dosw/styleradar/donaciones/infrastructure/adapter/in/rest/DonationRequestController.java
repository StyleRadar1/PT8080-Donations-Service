package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.dosw.styleradar.donaciones.application.dto.request.ConfirmReceiptRequestDTO;
import edu.dosw.styleradar.donaciones.application.dto.request.CreateDonationRequestDTO;
import edu.dosw.styleradar.donaciones.application.dto.request.DecideDonationRequestDTO;
import edu.dosw.styleradar.donaciones.application.dto.request.SchedulePickupRequestDTO;
import edu.dosw.styleradar.donaciones.application.dto.response.DonationRequestResponseDTO;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequestStatus;
import edu.dosw.styleradar.donaciones.domain.port.in.ConfirmDonationReceiptUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.CreateDonationRequestCommand;
import edu.dosw.styleradar.donaciones.domain.port.in.CreateDonationRequestUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.DecideDonationRequestUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.ScheduleDonationPickupUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.SchedulePickupCommand;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest.mapper.DonationRequestRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controlador REST para el ciclo de vida de solicitudes de donación de prendas hacia fundaciones aliadas.
 */
@RestController
@RequestMapping("/donaciones")
@RequiredArgsConstructor
@Tag(name = "Solicitudes de Donación", description = "Endpoints para la gestión del flujo y logística de donaciones de ropa a fundaciones")
public class DonationRequestController {

    private final CreateDonationRequestUseCase createDonationRequestUseCase;
    private final DecideDonationRequestUseCase decideDonationRequestUseCase;
    private final ScheduleDonationPickupUseCase scheduleDonationPickupUseCase;
    private final ConfirmDonationReceiptUseCase confirmDonationReceiptUseCase;
    private final DonationRequestRestMapper donationRequestRestMapper;

    @Operation(summary = "Crear solicitud de donación", description = "Inicia un trámite de donación para una prenda destinada previamente a donación hacia una fundación elegida")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Solicitud de donación creada exitosamente",
                    content = @Content(schema = @Schema(implementation = DonationRequestResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de solicitud inválidos"),
            @ApiResponse(responseCode = "404", description = "Publicación no encontrada"),
            @ApiResponse(responseCode = "409", description = "La publicación no está destinada a donación o ya tiene una solicitud activa")
    })
    @PostMapping
    public ResponseEntity<DonationRequestResponseDTO> createDonationRequest(
            @Valid @RequestBody CreateDonationRequestDTO request) {
        CreateDonationRequestCommand command = donationRequestRestMapper.toCommand(request);
        DonationRequest created = createDonationRequestUseCase.create(command);
        DonationRequestResponseDTO response = donationRequestRestMapper.toResponse(created);
        return ResponseEntity.created(URI.create("/api/v1/donaciones/" + response.id())).body(response);
    }

    @Operation(summary = "Obtener solicitud de donación por ID", description = "Recupera los datos y estado detallado de una solicitud de donación")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitud encontrada",
                    content = @Content(schema = @Schema(implementation = DonationRequestResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<DonationRequestResponseDTO> getDonationRequestById(
            @Parameter(description = "ID de la solicitud de donación", example = "1")
            @PathVariable Long id) {
        DonationRequest request = createDonationRequestUseCase.getById(id);
        return ResponseEntity.ok(donationRequestRestMapper.toResponse(request));
    }

    @Operation(summary = "Listar solicitudes de donación por donante", description = "Obtiene las solicitudes creadas por el usuario donante")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de solicitudes del donante",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DonationRequestResponseDTO.class))))
    })
    @GetMapping("/donor/{donorId}")
    public ResponseEntity<List<DonationRequestResponseDTO>> getRequestsByDonor(
            @Parameter(description = "ID del usuario donante", example = "101")
            @PathVariable Long donorId) {
        List<DonationRequest> list = createDonationRequestUseCase.getByDonorId(donorId);
        return ResponseEntity.ok(donationRequestRestMapper.toResponseList(list));
    }

    @Operation(summary = "Listar solicitudes de donación por fundación", description = "Obtiene las solicitudes dirigidas a una fundación, con filtro opcional por estado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de solicitudes de la fundación",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DonationRequestResponseDTO.class))))
    })
    @GetMapping("/foundation/{foundationId}")
    public ResponseEntity<List<DonationRequestResponseDTO>> getRequestsByFoundation(
            @Parameter(description = "ID de la fundación destinataria", example = "501")
            @PathVariable Long foundationId,
            @Parameter(description = "Estado opcional para filtrar solicitudes", example = "PENDING")
            @RequestParam(required = false) DonationRequestStatus status) {
        List<DonationRequest> list = createDonationRequestUseCase.getByFoundationId(foundationId, status);
        return ResponseEntity.ok(donationRequestRestMapper.toResponseList(list));
    }

    @Operation(summary = "Decidir sobre una solicitud de donación", description = "Permite a la fundación aprobar o rechazar la recepción de la prenda donada")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Decisión registrada exitosamente",
                    content = @Content(schema = @Schema(implementation = DonationRequestResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros de decisión inválidos"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
            @ApiResponse(responseCode = "409", description = "Conflicto con la fundación o la solicitud no está pendiente")
    })
    @PatchMapping("/{id}/decide")
    public ResponseEntity<DonationRequestResponseDTO> decideDonationRequest(
            @Parameter(description = "ID de la solicitud a decidir", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody DecideDonationRequestDTO request) {
        DonationRequest decided = decideDonationRequestUseCase.decide(
                request.foundationId(),
                id,
                request.approve(),
                request.reason()
        );
        return ResponseEntity.ok(donationRequestRestMapper.toResponse(decided));
    }

    @Operation(summary = "Programar recogida y franja horaria", description = "El donante agenda el punto de recogida/entrega y la franja de tiempo para una donación aprobada")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recogida agendada exitosamente",
                    content = @Content(schema = @Schema(implementation = DonationRequestResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos o franja de tiempo inválidos"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
            @ApiResponse(responseCode = "409", description = "Conflicto con el usuario o la solicitud no se encuentra en estado aprobada")
    })
    @PatchMapping("/{id}/pickup")
    public ResponseEntity<DonationRequestResponseDTO> schedulePickup(
            @Parameter(description = "ID de la solicitud de donación aprobada", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody SchedulePickupRequestDTO request) {
        SchedulePickupCommand command = donationRequestRestMapper.toCommand(id, request);
        DonationRequest scheduled = scheduleDonationPickupUseCase.schedulePickup(command);
        return ResponseEntity.ok(donationRequestRestMapper.toResponse(scheduled));
    }

    @Operation(summary = "Confirmar recepción física de la prenda", description = "La fundación confirma haber recibido la prenda donada concluyendo el ciclo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recepción confirmada exitosamente",
                    content = @Content(schema = @Schema(implementation = DonationRequestResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de confirmación inválidos"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
            @ApiResponse(responseCode = "409", description = "Conflicto con la fundación o la solicitud no está en estado de recogida")
    })
    @PatchMapping("/{id}/confirm")
    public ResponseEntity<DonationRequestResponseDTO> confirmReceipt(
            @Parameter(description = "ID de la solicitud de donación", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody ConfirmReceiptRequestDTO request) {
        DonationRequest confirmed = confirmDonationReceiptUseCase.confirmReceipt(request.foundationId(), id);
        return ResponseEntity.ok(donationRequestRestMapper.toResponse(confirmed));
    }
}
