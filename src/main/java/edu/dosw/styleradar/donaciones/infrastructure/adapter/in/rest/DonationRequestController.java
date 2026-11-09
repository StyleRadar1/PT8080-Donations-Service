package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest;

import java.net.URI;
import java.util.List;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controlador REST para el ciclo de vida de solicitudes de donación de prendas hacia fundaciones aliadas.
 */
@RestController
@RequestMapping("/donaciones")
@RequiredArgsConstructor
@Tag(name = "Solicitudes de Donación", description = "Endpoints para la gestión del flujo y logística de donaciones de ropa a fundaciones aliadas (SR-RF-44 a SR-RF-49)")
public class DonationRequestController {

    private final CreateDonationRequestUseCase createDonationRequestUseCase;
    private final DecideDonationRequestUseCase decideDonationRequestUseCase;
    private final ScheduleDonationPickupUseCase scheduleDonationPickupUseCase;
    private final ConfirmDonationReceiptUseCase confirmDonationReceiptUseCase;
    private final DonationRequestRestMapper donationRequestRestMapper;

    @Operation(
            summary = "Crear solicitud de donación (SR-RF-44, SR-RF-45)",
            description = "Inicia un trámite formal de donación para una prenda previamente destinada a donación, seleccionando una fundación aliada. Dispara evento de dominio y queda en estado PENDING. Requiere rol USER o ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Solicitud de donación creada exitosamente",
                    content = @Content(schema = @Schema(implementation = DonationRequestResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de solicitud inválidos o faltantes",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado o token JWT inválido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: rol insuficiente",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Publicación no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "La publicación no está destinada a donación o ya tiene una solicitud activa",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<DonationRequestResponseDTO> createDonationRequest(
            @Valid @RequestBody CreateDonationRequestDTO request) {
        CreateDonationRequestCommand command = donationRequestRestMapper.toCommand(request);
        DonationRequest created = createDonationRequestUseCase.create(command);
        DonationRequestResponseDTO response = donationRequestRestMapper.toResponse(created);
        return ResponseEntity.created(URI.create("/api/v1/donaciones/" + response.id())).body(response);
    }

    @Operation(
            summary = "Obtener solicitud de donación por ID (SR-RF-46)",
            description = "Recupera los datos y estado detallado de una solicitud de donación particular. Accesible por USER, FOUNDATION o ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitud encontrada",
                    content = @Content(schema = @Schema(implementation = DonationRequestResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado o token JWT inválido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: rol insuficiente",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'FOUNDATION', 'ADMIN')")
    public ResponseEntity<DonationRequestResponseDTO> getDonationRequestById(
            @Parameter(description = "ID de la solicitud de donación", example = "1")
            @PathVariable Long id) {
        DonationRequest request = createDonationRequestUseCase.getById(id);
        return ResponseEntity.ok(donationRequestRestMapper.toResponse(request));
    }

    @Operation(
            summary = "Listar solicitudes de donación por donante (SR-RF-46)",
            description = "Obtiene el historial de solicitudes benéficas creadas por el usuario donante. Requiere rol USER o ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de solicitudes del donante",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DonationRequestResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado o token JWT inválido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: rol insuficiente",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/donor/{donorId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<DonationRequestResponseDTO>> getRequestsByDonor(
            @Parameter(description = "ID del usuario donante", example = "101")
            @PathVariable Long donorId) {
        List<DonationRequest> list = createDonationRequestUseCase.getByDonorId(donorId);
        return ResponseEntity.ok(donationRequestRestMapper.toResponseList(list));
    }

    @Operation(
            summary = "Listar solicitudes de donación por fundación (SR-RF-46)",
            description = "Obtiene las solicitudes dirigidas a una fundación aliada, con filtro opcional por estado en el ciclo de vida. Requiere rol FOUNDATION o ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de solicitudes de la fundación",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DonationRequestResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado o token JWT inválido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: rol insuficiente",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/foundation/{foundationId}")
    @PreAuthorize("hasAnyRole('FOUNDATION', 'ADMIN')")
    public ResponseEntity<List<DonationRequestResponseDTO>> getRequestsByFoundation(
            @Parameter(description = "ID de la fundación destinataria", example = "501")
            @PathVariable Long foundationId,
            @Parameter(description = "Estado opcional para filtrar solicitudes (PENDING, APPROVED, REJECTED, IN_PICKUP, DELIVERED)", example = "PENDING")
            @RequestParam(required = false) DonationRequestStatus status) {
        List<DonationRequest> list = createDonationRequestUseCase.getByFoundationId(foundationId, status);
        return ResponseEntity.ok(donationRequestRestMapper.toResponseList(list));
    }

    @Operation(
            summary = "Decidir sobre una solicitud de donación (SR-RF-47)",
            description = "Permite a la fundación aliada receptora aprobar (APPROVED) o rechazar (REJECTED con justificación obligatoria) la recepción de la prenda donada. Requiere rol FOUNDATION o ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Decisión registrada exitosamente",
                    content = @Content(schema = @Schema(implementation = DonationRequestResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros de decisión inválidos",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado o token JWT inválido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: rol insuficiente",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflicto: la solicitud no está dirigida a esta fundación o ya no se encuentra pendiente",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{id}/decide")
    @PreAuthorize("hasAnyRole('FOUNDATION', 'ADMIN')")
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

    @Operation(
            summary = "Coordinar recogida y franja horaria (SR-RF-48)",
            description = "El usuario donante agenda el punto de recogida/entrega física y la franja de tiempo para una donación aprobada (transiciona a IN_PICKUP). Requiere rol USER o ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recogida agendada exitosamente",
                    content = @Content(schema = @Schema(implementation = DonationRequestResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos o franja de tiempo inválidos",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado o token JWT inválido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: rol insuficiente",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflicto: el usuario no es el donante o la solicitud no se encuentra aprobada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{id}/pickup")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<DonationRequestResponseDTO> schedulePickup(
            @Parameter(description = "ID de la solicitud de donación aprobada", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody SchedulePickupRequestDTO request) {
        SchedulePickupCommand command = donationRequestRestMapper.toCommand(id, request);
        DonationRequest scheduled = scheduleDonationPickupUseCase.schedulePickup(command);
        return ResponseEntity.ok(donationRequestRestMapper.toResponse(scheduled));
    }

    @Operation(
            summary = "Confirmar recepción física de la donación (SR-RF-49)",
            description = "La fundación aliada confirma haber recibido la prenda donada, culminando el ciclo a ENTREGADA (DELIVERED) y actualizando la publicación a DONADA (DONATED). Requiere rol FOUNDATION o ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recepción confirmada exitosamente y ciclo completado",
                    content = @Content(schema = @Schema(implementation = DonationRequestResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de confirmación inválidos",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado o token JWT inválido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: rol insuficiente",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflicto: la fundación no corresponde o la solicitud no está en estado de recogida",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('FOUNDATION', 'ADMIN')")
    public ResponseEntity<DonationRequestResponseDTO> confirmReceipt(
            @Parameter(description = "ID de la solicitud de donación", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody ConfirmReceiptRequestDTO request) {
        DonationRequest confirmed = confirmDonationReceiptUseCase.confirmReceipt(request.foundationId(), id);
        return ResponseEntity.ok(donationRequestRestMapper.toResponse(confirmed));
    }
}
