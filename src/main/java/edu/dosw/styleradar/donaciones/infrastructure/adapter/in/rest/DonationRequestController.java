package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
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

import edu.dosw.styleradar.donaciones.application.dto.request.ClaimDonationRequestDTO;
import edu.dosw.styleradar.donaciones.application.dto.request.ConfirmReceiptRequestDTO;
import edu.dosw.styleradar.donaciones.application.dto.request.CreateDonationRequestDTO;
import edu.dosw.styleradar.donaciones.application.dto.request.DecideDonationRequestDTO;
import edu.dosw.styleradar.donaciones.application.dto.request.SchedulePickupRequestDTO;
import edu.dosw.styleradar.donaciones.application.dto.response.DonationRequestResponseDTO;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequestStatus;
import edu.dosw.styleradar.donaciones.domain.port.in.ClaimDonationCommand;
import edu.dosw.styleradar.donaciones.domain.port.in.ClaimDonationUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.ConfirmDonationReceiptUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.CreateDonationRequestCommand;
import edu.dosw.styleradar.donaciones.domain.port.in.CreateDonationRequestUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.DecideDonationRequestUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.ScheduleDonationPickupUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.SchedulePickupCommand;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest.mapper.DonationRequestRestMapper;
import edu.dosw.styleradar.donaciones.infrastructure.security.ResourceOwnershipValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controlador REST para el ciclo benéfico y logístico de Donaciones hacia fundaciones aliadas en Bogotá D.C.
 * Soporta radicación tradicional y reclamo directo desde la Vitrina Abierta con control de concurrencia optimista.
 */
@RestController
@RequestMapping({"/donaciones", "/donations"})
@RequiredArgsConstructor
@Tag(
        name = "Donaciones - Vitrina Benéfica y Logística",
        description = "Endpoints para el flujo de donaciones de ropa a fundaciones aliadas en Bogotá D.C., reclamo en Vitrina Abierta y logística de entrega (SR-RF-44 a SR-RF-49)"
)
public class DonationRequestController {

    private final CreateDonationRequestUseCase createDonationRequestUseCase;
    private final ClaimDonationUseCase claimDonationUseCase;
    private final DecideDonationRequestUseCase decideDonationRequestUseCase;
    private final ScheduleDonationPickupUseCase scheduleDonationPickupUseCase;
    private final ConfirmDonationReceiptUseCase confirmDonationReceiptUseCase;
    private final DonationRequestRestMapper donationRequestRestMapper;
    private final ResourceOwnershipValidator resourceOwnershipValidator;

    @Operation(
            summary = "Reclamar donación desde la Vitrina Abierta (Fundaciones aliadas)",
            description = "Permite a una fundación registrada reclamar directamente una prenda disponible en la Vitrina Benéfica (esDonacion=true). Bloqueo optimista (@Version): si dos fundaciones reclaman al mismo tiempo, solo una gana y la otra recibe 409 Conflict. Una fundación no puede reclamar su propia donación. Requiere rol FOUNDATION o ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Donación reclamada exitosamente. Solicitud aprobada automáticamente para coordinar logística",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DonationRequestResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "ReclamoExitoso",
                                    value = """
                                    {
                                      "id": 1,
                                      "publicationId": 2,
                                      "donorId": 102,
                                      "foundationId": 501,
                                      "message": "Aceptamos esta prenda para el programa de abrigo a la niñez en Ciudad Bolívar",
                                      "status": "APPROVED",
                                      "rejectionReason": null,
                                      "deliveryPoint": null,
                                      "slotStart": null,
                                      "slotEnd": null,
                                      "version": 0,
                                      "requestDate": "2026-11-11T01:30:00",
                                      "decisionDate": "2026-11-11T01:30:00",
                                      "receptionDate": null
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Solicitud inválida: prenda no es de donación o intento de auto-donación",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "ErrorAutoDonacion",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/bad-request",
                                      "title": "Solicitud inválida",
                                      "status": 400,
                                      "detail": "Regla de negocio: Una fundación no puede reclamar o aceptar su propia donación",
                                      "instance": "/api/v1/donations/claim",
                                      "timestamp": "2026-11-11T01:30:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado: rol FOUNDATION requerido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Publicación no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflicto concurrente: prenda ya reclamada por otra fundación",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "ConflictoConcurrencia",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/conflict",
                                      "title": "Conflicto de dominio",
                                      "status": 409,
                                      "detail": "La prenda ya fue reclamada concurrentemente por otra fundación o cambió de estado.",
                                      "instance": "/api/v1/donations/claim",
                                      "timestamp": "2026-11-11T01:30:00Z"
                                    }
                                    """
                            )
                    )
            )
    })
    @PostMapping({"/claim", "/requests/claim"})
    @PreAuthorize("hasAnyRole('FOUNDATION', 'ADMIN')")
    public ResponseEntity<DonationRequestResponseDTO> claimDonation(
            @Valid @RequestBody ClaimDonationRequestDTO requestDTO) {
        resourceOwnershipValidator.validateOwnerOrAdmin(requestDTO.foundationId());
        ClaimDonationCommand command = donationRequestRestMapper.toCommand(requestDTO);
        DonationRequest request = claimDonationUseCase.claimDonation(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(donationRequestRestMapper.toResponse(request));
    }

    @Operation(
            summary = "Crear solicitud directa de donación (SR-RF-44, SR-RF-45)",
            description = "El donante postula una prenda al canal benéfico dirigiéndola hacia una fundación específica. Queda en estado PENDING y dispara evento de integración. Requiere rol USER, STORE o ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Solicitud de donación creada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DonationRequestResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "SolicitudCreada",
                                    value = """
                                    {
                                      "id": 1,
                                      "publicationId": 10,
                                      "donorId": 101,
                                      "foundationId": 501,
                                      "message": "Deseo donar esta chaqueta para los programas comunitarios en Bogotá",
                                      "status": "PENDING",
                                      "rejectionReason": null,
                                      "deliveryPoint": null,
                                      "slotStart": null,
                                      "slotEnd": null,
                                      "version": 0,
                                      "requestDate": "2026-11-11T01:30:00",
                                      "decisionDate": null,
                                      "receptionDate": null
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: no es el donante autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Publicación no encontrada", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "La prenda ya tiene una solicitud activa o no está marcada para donar", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping({"", "/requests"})
    @PreAuthorize("hasAnyRole('USER', 'STORE', 'FOUNDATION', 'ADMIN')")
    public ResponseEntity<DonationRequestResponseDTO> createDonationRequest(
            @Valid @RequestBody CreateDonationRequestDTO requestDTO) {
        resourceOwnershipValidator.validateOwnerOrAdmin(requestDTO.donorId());
        CreateDonationRequestCommand command = donationRequestRestMapper.toCommand(requestDTO);
        DonationRequest request = createDonationRequestUseCase.create(command);
        DonationRequestResponseDTO response = donationRequestRestMapper.toResponse(request);
        return ResponseEntity.created(URI.create("/api/v1/donaciones/" + request.getId())).body(response);
    }

    @Operation(
            summary = "Consultar solicitud de donación por ID",
            description = "Devuelve el estado, fechas y detalles logísticos de una solicitud de donación."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Detalle de la solicitud encontrado",
                    content = @Content(schema = @Schema(implementation = DonationRequestResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Solicitud no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @GetMapping({"/{id}", "/requests/{id}"})
    public ResponseEntity<DonationRequestResponseDTO> getDonationRequestById(
            @Parameter(description = "ID de la solicitud de donación", example = "1")
            @PathVariable Long id) {
        DonationRequest request = createDonationRequestUseCase.getById(id);
        return ResponseEntity.ok(donationRequestRestMapper.toResponse(request));
    }

    @Operation(
            summary = "Evaluar solicitud de donación (Fundación - SR-RF-46)",
            description = "Permite a la fundación receptora aceptar (APPROVED) o rechazar (REJECTED con motivo) la solicitud. Requiere rol FOUNDATION o ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Decisión registrada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DonationRequestResponseDTO.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Motivo de rechazo faltante al declinar", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "No es la fundación destinataria", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "La solicitud no se encuentra en estado PENDING", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping({"/{id}/decide", "/requests/{id}/decide"})
    @PreAuthorize("hasAnyRole('FOUNDATION', 'ADMIN')")
    public ResponseEntity<DonationRequestResponseDTO> decideDonationRequest(
            @Parameter(description = "ID de la solicitud", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody DecideDonationRequestDTO requestDTO) {
        resourceOwnershipValidator.validateOwnerOrAdmin(requestDTO.foundationId());
        DonationRequest updated = decideDonationRequestUseCase.decide(
                requestDTO.foundationId(),
                id,
                requestDTO.approved(),
                requestDTO.rejectionReason()
        );
        return ResponseEntity.ok(donationRequestRestMapper.toResponse(updated));
    }

    @Operation(
            summary = "Programar recogida o entrega logística (SR-RF-47, SR-RF-48)",
            description = "Fija el punto de entrega en Bogotá D.C. y la franja horaria para recibir las prendas aprobadas (estado IN_PICKUP). Publica evento en RabbitMQ.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Cita de recogida programada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DonationRequestResponseDTO.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Franja horaria o punto de entrega inválidos", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "No autorizado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "La solicitud no está en estado APPROVED o IN_PICKUP", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping({"/{id}/pickup", "/requests/{id}/pickup"})
    @PreAuthorize("hasAnyRole('USER', 'FOUNDATION', 'ADMIN')")
    public ResponseEntity<DonationRequestResponseDTO> schedulePickup(
            @Parameter(description = "ID de la solicitud", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody SchedulePickupRequestDTO requestDTO) {
        resourceOwnershipValidator.validateOwnerOrAdmin(requestDTO.userId());
        SchedulePickupCommand command = donationRequestRestMapper.toCommand(id, requestDTO);
        DonationRequest updated = scheduleDonationPickupUseCase.schedulePickup(command);
        return ResponseEntity.ok(donationRequestRestMapper.toResponse(updated));
    }

    @Operation(
            summary = "Confirmar entrega física de la donación (SR-RF-49)",
            description = "La fundación confirma la recepción de la prenda (estado DELIVERED), actualizando la publicación a DONATED y cerrando el ciclo benéfico con evento RabbitMQ.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Entrega física confirmada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DonationRequestResponseDTO.class)
                    )
            ),
            @ApiResponse(responseCode = "403", description = "No es la fundación receptora", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "La solicitud no se encuentra en estado IN_PICKUP", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping({"/{id}/confirm", "/{id}/confirm-receipt", "/requests/{id}/confirm", "/requests/{id}/confirm-receipt"})
    @PreAuthorize("hasAnyRole('FOUNDATION', 'ADMIN')")
    public ResponseEntity<DonationRequestResponseDTO> confirmReceipt(
            @Parameter(description = "ID de la solicitud", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody ConfirmReceiptRequestDTO requestDTO) {
        resourceOwnershipValidator.validateOwnerOrAdmin(requestDTO.foundationId());
        DonationRequest updated = confirmDonationReceiptUseCase.confirmReceipt(requestDTO.foundationId(), id);
        return ResponseEntity.ok(donationRequestRestMapper.toResponse(updated));
    }

    @Operation(
            summary = "Consultar historial de donaciones del donante",
            description = "Lista todas las solicitudes originadas por un usuario o tienda donante.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Historial del donante", content = @Content(array = @ArraySchema(schema = @Schema(implementation = DonationRequestResponseDTO.class)))),
            @ApiResponse(responseCode = "403", description = "No es el donante autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping({"/donor/{donorId}", "/donante/{donorId}", "/requests/donor/{donorId}", "/requests/donante/{donorId}"})
    @PreAuthorize("hasAnyRole('USER', 'STORE', 'FOUNDATION', 'ADMIN')")
    public ResponseEntity<List<DonationRequestResponseDTO>> getByDonorId(
            @Parameter(description = "ID del donante", example = "101")
            @PathVariable Long donorId) {
        resourceOwnershipValidator.validateOwnerOrAdmin(donorId);
        List<DonationRequest> list = createDonationRequestUseCase.getByDonorId(donorId);
        return ResponseEntity.ok(donationRequestRestMapper.toResponseList(list));
    }

    @Operation(
            summary = "Consultar bandeja de solicitudes de la fundación",
            description = "Lista solicitudes dirigidas a una fundación aliada con filtro opcional de estado.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bandeja de la fundación", content = @Content(array = @ArraySchema(schema = @Schema(implementation = DonationRequestResponseDTO.class)))),
            @ApiResponse(responseCode = "403", description = "No es la fundación autenticada", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping({"/foundation/{foundationId}", "/fundacion/{foundationId}", "/requests/foundation/{foundationId}", "/requests/fundacion/{foundationId}"})
    @PreAuthorize("hasAnyRole('FOUNDATION', 'ADMIN')")
    public ResponseEntity<List<DonationRequestResponseDTO>> getByFoundationId(
            @Parameter(description = "ID de la fundación", example = "501")
            @PathVariable Long foundationId,
            @Parameter(description = "Filtro de estado opcional", example = "PENDING")
            @RequestParam(required = false) DonationRequestStatus status) {
        resourceOwnershipValidator.validateOwnerOrAdmin(foundationId);
        List<DonationRequest> list = createDonationRequestUseCase.getByFoundationId(foundationId, status);
        return ResponseEntity.ok(donationRequestRestMapper.toResponseList(list));
    }
}
