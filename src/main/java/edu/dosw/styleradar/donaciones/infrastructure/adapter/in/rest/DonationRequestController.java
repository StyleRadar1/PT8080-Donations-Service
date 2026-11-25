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
import edu.dosw.styleradar.donaciones.application.dto.response.FoundationResponseDTO;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidDonationRequestException;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequestStatus;
import edu.dosw.styleradar.donaciones.domain.model.FoundationSummary;
import edu.dosw.styleradar.donaciones.domain.port.in.ClaimDonationCommand;
import edu.dosw.styleradar.donaciones.domain.port.in.ClaimDonationUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.ConfirmDonationReceiptUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.CreateDonationRequestCommand;
import edu.dosw.styleradar.donaciones.domain.port.in.CreateDonationRequestUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.DecideDonationRequestUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.GetPartnerFoundationsUseCase;
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
@RequestMapping("/api/v1/donations")
@RequiredArgsConstructor
@Tag(
        name = "Charitable Donations & Logistics",
        description = "Endpoints for garment donations to partner foundations in Bogota D.C., open showcase claims, and pickup logistics"
)
public class DonationRequestController {

    private final CreateDonationRequestUseCase createDonationRequestUseCase;
    private final ClaimDonationUseCase claimDonationUseCase;
    private final DecideDonationRequestUseCase decideDonationRequestUseCase;
    private final ScheduleDonationPickupUseCase scheduleDonationPickupUseCase;
    private final ConfirmDonationReceiptUseCase confirmDonationReceiptUseCase;
    private final GetPartnerFoundationsUseCase getPartnerFoundationsUseCase;
    private final DonationRequestRestMapper donationRequestRestMapper;
    private final ResourceOwnershipValidator resourceOwnershipValidator;

    @Operation(
            summary = "List active partner foundations",
            description = "Queries active partner foundations from ms-tiendas to populate the donation target selector, with optional garment category filtering (Camisas, Camisetas, Suéteres, Chaquetas, Pantalones, Faldas, Vestidos, Calzado, Accesorios, or SUPERIOR/INFERIOR/CALZADO/ACCESORIOS).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Alphabetically sorted list of active partner foundations",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = FoundationResponseDTO.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "ms-tiendas temporarily unavailable",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @GetMapping("/foundations")
    @PreAuthorize("hasAnyRole('USER', 'STORE', 'FOUNDATION', 'ADMIN')")
    public ResponseEntity<List<FoundationResponseDTO>> getActiveFoundations(
            @Parameter(description = "Optional garment category to filter compatible foundations", example = "Chaquetas")
            @RequestParam(required = false) String category) {
        List<FoundationSummary> foundations = getPartnerFoundationsUseCase.getActiveFoundations(category);
        return ResponseEntity.ok(donationRequestRestMapper.toFoundationResponseList(foundations));
    }

    @Operation(
            summary = "Get authenticated representative's foundation",
            description = "Resolves the active partner foundation from ms-tiendas whose representanteId matches the authenticated user ID (JWT sub claim).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Partner foundation associated with the authenticated representative",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = FoundationResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No active foundation linked to the authenticated user",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "ms-tiendas temporarily unavailable",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @GetMapping("/foundations/representative")
    @PreAuthorize("hasAnyRole('FOUNDATION', 'ADMIN')")
    public ResponseEntity<FoundationResponseDTO> getRepresentativeFoundation() {
        Long authenticatedUserId = resourceOwnershipValidator.extractAuthenticatedUserId();
        FoundationSummary foundation = getPartnerFoundationsUseCase.getFoundationByRepresentativeId(authenticatedUserId);
        return ResponseEntity.ok(donationRequestRestMapper.toFoundationResponse(foundation));
    }

    @Operation(
            summary = "Get active partner foundation details by ID",
            description = "Queries ms-tiendas for the institutional details of a partner foundation and verifies that it is currently active.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Active partner foundation found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = FoundationResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Foundation exists but is inactive",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Foundation not found in ms-tiendas",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "ms-tiendas temporarily unavailable",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @GetMapping("/foundations/{foundationId}")
    @PreAuthorize("hasAnyRole('USER', 'STORE', 'FOUNDATION', 'ADMIN')")
    public ResponseEntity<FoundationResponseDTO> getActiveFoundationById(
            @Parameter(description = "Foundation ID in ms-tiendas", example = "3")
            @PathVariable Long foundationId) {
        FoundationSummary foundation = getPartnerFoundationsUseCase.getActiveFoundationById(foundationId);
        return ResponseEntity.ok(donationRequestRestMapper.toFoundationResponse(foundation));
    }

    @Operation(
            summary = "Claim available garment from Open Showcase",
            description = "Allows a registered partner foundation to directly claim an available garment in the Charitable Showcase (esDonacion=true). Uses optimistic locking (@Version): if two foundations claim simultaneously, only one succeeds and the other receives 409 Conflict. Prevents self-donation. Requires FOUNDATION or ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Donation successfully claimed; request automatically approved for pickup coordination",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DonationRequestResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "ClaimSuccessSample",
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
                                      "version": 1,
                                      "requestDate": "2026-11-11T10:00:00",
                                      "decisionDate": "2026-11-11T10:00:00",
                                      "receptionDate": null
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request: garment is not marked for donation or self-donation attempt",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "SelfDonationError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/bad-request",
                                      "title": "Solicitud inválida",
                                      "status": 400,
                                      "detail": "Regla de negocio: Una fundación no puede reclamar o aceptar su propia donación",
                                      "instance": "/api/v1/donations/claims",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthenticated: missing or invalid JWT token",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "UnauthorizedError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/unauthorized",
                                      "title": "No autenticado",
                                      "status": 401,
                                      "detail": "Token Bearer JWT ausente, expirado o con firma no válida.",
                                      "instance": "/api/v1/donations/claims",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden: FOUNDATION or ADMIN role required",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "InsufficientRoleError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/forbidden",
                                      "title": "Acceso denegado",
                                      "status": 403,
                                      "detail": "Acceso denegado: Se requiere rol FOUNDATION o ADMIN para reclamar donaciones de la vitrina.",
                                      "instance": "/api/v1/donations/claims",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Publication not found",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "PublicationNotFoundError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/not-found",
                                      "title": "Recurso no encontrado",
                                      "status": 404,
                                      "detail": "No existe publicación con ID: 999 en el catálogo de donaciones",
                                      "instance": "/api/v1/donations/claims",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Concurrency conflict: garment already claimed by another foundation",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "ConcurrencyConflictError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/conflict",
                                      "title": "Conflicto de dominio",
                                      "status": 409,
                                      "detail": "La prenda ya fue reclamada concurrentemente por otra fundación o cambió de estado.",
                                      "instance": "/api/v1/donations/claims",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "ClaimServerError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/internal-server-error",
                                      "title": "Error interno del servidor",
                                      "status": 500,
                                      "detail": "Ocurrió un error inesperado al procesar el reclamo de la donación.",
                                      "instance": "/api/v1/donations/claims",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            )
    })
    @PostMapping("/claims")
    @PreAuthorize("hasAnyRole('FOUNDATION', 'ADMIN')")
    public ResponseEntity<DonationRequestResponseDTO> claimDonation(
            @Valid @RequestBody ClaimDonationRequestDTO requestDTO) {
        resourceOwnershipValidator.validateFoundationOwnership(requestDTO.foundationId());
        ClaimDonationCommand command = donationRequestRestMapper.toCommand(requestDTO);
        DonationRequest request = claimDonationUseCase.claimDonation(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(donationRequestRestMapper.toResponse(request));
    }

    @Operation(
            summary = "Create donation request with target foundation and delivery proposal",
            description = "Submits a garment to an active partner foundation including the initial deliveryProposal (deliveryMode, deliveryPoint, startTime, endTime) or legacy flat fields. The donor identity (donorId) is obtained exclusively from the authenticated JWT session token, ignoring any client-supplied donorId. Created in PENDING state and emits an integration event. Requires USER, STORE, or ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Donation request created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DonationRequestResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "DonationRequestCreatedSample",
                                    value = """
                                    {
                                      "id": 1,
                                      "publicationId": 10,
                                      "donorId": 101,
                                      "foundationId": 3,
                                      "message": "Deseo donar esta chaqueta para los programas comunitarios en Bogotá",
                                      "status": "PENDING",
                                      "rejectionReason": null,
                                      "deliveryMode": "DROP_OFF_POINT",
                                      "deliveryPoint": "Calle 85 # 15-32, Chapinero, Bogotá",
                                      "slotStart": "2026-12-15T09:00:00",
                                      "slotEnd": "2026-12-15T12:00:00",
                                      "version": 0,
                                      "requestDate": "2026-11-11T10:00:00",
                                      "decisionDate": null,
                                      "receptionDate": null
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input payload",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "InvalidInputError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/bad-request",
                                      "title": "Solicitud inválida",
                                      "status": 400,
                                      "detail": "El identificador de publicación (publicationId) y de fundación (foundationId) son obligatorios.",
                                      "instance": "/api/v1/donations",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthenticated",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "UnauthorizedError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/unauthorized",
                                      "title": "No autenticado",
                                      "status": 401,
                                      "detail": "Token Bearer JWT ausente o inválido.",
                                      "instance": "/api/v1/donations",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden: user is not the authenticated donor",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "ForbiddenError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/forbidden",
                                      "title": "Acceso denegado",
                                      "status": 403,
                                      "detail": "El usuario autenticado (ID: 102) no es propietario de la prenda ni administrador.",
                                      "instance": "/api/v1/donations",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Publication not found",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "PublicationNotFoundError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/not-found",
                                      "title": "Recurso no encontrado",
                                      "status": 404,
                                      "detail": "No existe publicación con ID: 999",
                                      "instance": "/api/v1/donations",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Garment already has an active request or is not in donation state",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "DuplicateDonationConflict",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/conflict",
                                      "title": "Conflicto de dominio",
                                      "status": 409,
                                      "detail": "La publicación ID: 10 ya cuenta con una solicitud de donación activa en estado PENDING.",
                                      "instance": "/api/v1/donations",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "CreateDonationServerError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/internal-server-error",
                                      "title": "Error interno del servidor",
                                      "status": 500,
                                      "detail": "Error inesperado al registrar la solicitud de donación.",
                                      "instance": "/api/v1/donations",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            )
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'STORE', 'ADMIN')")
    public ResponseEntity<DonationRequestResponseDTO> createDonationRequest(
            @Valid @RequestBody CreateDonationRequestDTO requestDTO) {
        Long resolvedDonorId = resourceOwnershipValidator.extractAuthenticatedUserId();
        if (resolvedDonorId == null) {
            throw new InvalidDonationRequestException(
                    "El identificador del donante no está disponible en el token de sesión JWT");
        }
        CreateDonationRequestCommand command = donationRequestRestMapper.toCommand(requestDTO, resolvedDonorId);
        DonationRequest request = createDonationRequestUseCase.create(command);
        DonationRequestResponseDTO response = donationRequestRestMapper.toResponse(request);
        return ResponseEntity.created(URI.create("/api/v1/donations/" + request.getId())).body(response);
    }

    @Operation(
            summary = "Get donation request by ID",
            description = "Returns the status, timestamps, and pickup details of a specific donation request for authorized participants.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Donation request details found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DonationRequestResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "RequestFoundSample",
                                    value = """
                                    {
                                      "id": 1,
                                      "publicationId": 10,
                                      "donorId": 101,
                                      "foundationId": 501,
                                      "message": "Deseo donar esta chaqueta para los programas comunitarios en Bogotá",
                                      "status": "APPROVED",
                                      "rejectionReason": null,
                                      "deliveryPoint": "Sede Chapinero: Calle 67 # 11-45, Bogotá",
                                      "slotStart": "2026-11-15T09:00:00",
                                      "slotEnd": "2026-11-15T12:00:00",
                                      "version": 1,
                                      "requestDate": "2026-11-11T10:00:00",
                                      "decisionDate": "2026-11-11T14:30:00",
                                      "receptionDate": null
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Donation request not found",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "RequestNotFoundError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/not-found",
                                      "title": "Recurso no encontrado",
                                      "status": 404,
                                      "detail": "No existe solicitud de donación con ID: 999",
                                      "instance": "/api/v1/donations/999",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "GetRequestServerError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/internal-server-error",
                                      "title": "Error interno del servidor",
                                      "status": 500,
                                      "detail": "Error inesperado al consultar la solicitud de donación.",
                                      "instance": "/api/v1/donations/1",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            )
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'STORE', 'FOUNDATION', 'ADMIN')")
    public ResponseEntity<DonationRequestResponseDTO> getDonationRequestById(
            @Parameter(description = "Donation request ID", example = "1")
            @PathVariable Long id) {
        DonationRequest request = createDonationRequestUseCase.getById(id);
        resourceOwnershipValidator.validateDonationRequestAccess(request);
        return ResponseEntity.ok(donationRequestRestMapper.toResponse(request));
    }

    @Operation(
            summary = "Approve or reject donation request",
            description = "Allows the target foundation to approve (APPROVED) or reject (REJECTED with mandatory reason) a pending donation request. Requires FOUNDATION or ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Decision recorded successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DonationRequestResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "DecisionApprovedSample",
                                    value = """
                                    {
                                      "id": 1,
                                      "publicationId": 10,
                                      "donorId": 101,
                                      "foundationId": 501,
                                      "message": "Deseo donar esta chaqueta para los programas comunitarios en Bogotá",
                                      "status": "APPROVED",
                                      "rejectionReason": null,
                                      "deliveryPoint": null,
                                      "slotStart": null,
                                      "slotEnd": null,
                                      "version": 1,
                                      "requestDate": "2026-11-11T10:00:00",
                                      "decisionDate": "2026-11-11T14:30:00",
                                      "receptionDate": null
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing rejection reason when declining",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "MissingRejectionReasonError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/bad-request",
                                      "title": "Solicitud inválida",
                                      "status": 400,
                                      "detail": "Debe especificar el motivo de rechazo (rejectionReason) cuando la solicitud es denegada.",
                                      "instance": "/api/v1/donations/1/decision",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthenticated",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "UnauthorizedError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/unauthorized",
                                      "title": "No autenticado",
                                      "status": 401,
                                      "detail": "Token Bearer JWT ausente o inválido.",
                                      "instance": "/api/v1/donations/1/decision",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden: user does not represent the target foundation",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "NotTargetFoundationError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/forbidden",
                                      "title": "Acceso denegado",
                                      "status": 403,
                                      "detail": "La fundación autenticada (ID: 502) no es la destinataria de la solicitud ID: 1.",
                                      "instance": "/api/v1/donations/1/decision",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Donation request not found",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "RequestNotFoundError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/not-found",
                                      "title": "Recurso no encontrado",
                                      "status": 404,
                                      "detail": "No existe solicitud de donación con ID: 999",
                                      "instance": "/api/v1/donations/999/decision",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict: request is not in PENDING state",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "InvalidStateForDecisionError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/conflict",
                                      "title": "Conflicto de dominio",
                                      "status": 409,
                                      "detail": "La solicitud con ID: 1 no se encuentra en estado PENDING para ser evaluada.",
                                      "instance": "/api/v1/donations/1/decision",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "DecisionServerError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/internal-server-error",
                                      "title": "Error interno del servidor",
                                      "status": 500,
                                      "detail": "Error inesperado al registrar la decisión de la fundación.",
                                      "instance": "/api/v1/donations/1/decision",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            )
    })
    @PatchMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('FOUNDATION', 'ADMIN')")
    public ResponseEntity<DonationRequestResponseDTO> decideDonationRequest(
            @Parameter(description = "Donation request ID", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody DecideDonationRequestDTO requestDTO) {
        resourceOwnershipValidator.validateFoundationOwnership(requestDTO.foundationId());
        DonationRequest updated = decideDonationRequestUseCase.decide(
                requestDTO.foundationId(),
                id,
                requestDTO.approved(),
                requestDTO.rejectionReason()
        );
        return ResponseEntity.ok(donationRequestRestMapper.toResponse(updated));
    }

    @Operation(
            summary = "Schedule or reschedule donation pickup logistics",
            description = "Sets the delivery point in Bogota D.C. and pickup time slot for an approved or claimed donation request (transitions to IN_PICKUP). Publishes a RabbitMQ integration event.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Pickup appointment scheduled successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DonationRequestResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "PickupScheduledSample",
                                    value = """
                                    {
                                      "id": 1,
                                      "publicationId": 10,
                                      "donorId": 101,
                                      "foundationId": 501,
                                      "message": "Deseo donar esta chaqueta para los programas comunitarios en Bogotá",
                                      "status": "IN_PICKUP",
                                      "rejectionReason": null,
                                      "deliveryPoint": "Punto de Acopio StyleRadar: Calle 100 # 15-20, Usaquén, Bogotá",
                                      "slotStart": "2026-11-15T09:00:00",
                                      "slotEnd": "2026-11-15T12:00:00",
                                      "version": 2,
                                      "requestDate": "2026-11-11T10:00:00",
                                      "decisionDate": "2026-11-11T14:30:00",
                                      "receptionDate": null
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid time slot or delivery point",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "InvalidTimeSlotError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/bad-request",
                                      "title": "Solicitud inválida",
                                      "status": 400,
                                      "detail": "La hora de inicio (slotStart) debe ser estrictamente anterior a la hora de fin (slotEnd).",
                                      "instance": "/api/v1/donations/1/pickup",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthenticated",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "UnauthorizedError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/unauthorized",
                                      "title": "No autenticado",
                                      "status": 401,
                                      "detail": "Token Bearer JWT ausente o inválido.",
                                      "instance": "/api/v1/donations/1/pickup",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden: user is not authorized for this donation request",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "UnauthorizedForPickupError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/forbidden",
                                      "title": "Acceso denegado",
                                      "status": 403,
                                      "detail": "El usuario autenticado no está autorizado para agendar la recogida de esta solicitud.",
                                      "instance": "/api/v1/donations/1/pickup",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Donation request not found",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "RequestNotFoundError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/not-found",
                                      "title": "Recurso no encontrado",
                                      "status": 404,
                                      "detail": "No existe solicitud de donación con ID: 999",
                                      "instance": "/api/v1/donations/999/pickup",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict: request is not in APPROVED or IN_PICKUP status",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "InvalidStateForPickupError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/conflict",
                                      "title": "Conflicto de dominio",
                                      "status": 409,
                                      "detail": "La solicitud con ID: 1 debe estar en estado APPROVED para agendar logística de recogida.",
                                      "instance": "/api/v1/donations/1/pickup",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "PickupServerError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/internal-server-error",
                                      "title": "Error interno del servidor",
                                      "status": 500,
                                      "detail": "Error inesperado al agendar la recogida de la donación.",
                                      "instance": "/api/v1/donations/1/pickup",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            )
    })
    @PatchMapping("/{id}/pickup")
    @PreAuthorize("hasAnyRole('USER', 'STORE', 'FOUNDATION', 'ADMIN')")
    public ResponseEntity<DonationRequestResponseDTO> schedulePickup(
            @Parameter(description = "Donation request ID", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody SchedulePickupRequestDTO requestDTO) {
        resourceOwnershipValidator.validateOwnerOrAdmin(requestDTO.userId());
        SchedulePickupCommand command = donationRequestRestMapper.toCommand(id, requestDTO);
        DonationRequest updated = scheduleDonationPickupUseCase.schedulePickup(command);
        return ResponseEntity.ok(donationRequestRestMapper.toResponse(updated));
    }

    @Operation(
            summary = "Confirm physical delivery of donation",
            description = "Allows the receiving foundation to confirm physical garment reception (DELIVERED), transitioning the publication to DONATED and publishing a completion RabbitMQ event.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Physical delivery confirmed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DonationRequestResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "DeliveryConfirmedSample",
                                    value = """
                                    {
                                      "id": 1,
                                      "publicationId": 10,
                                      "donorId": 101,
                                      "foundationId": 501,
                                      "message": "Deseo donar esta chaqueta para los programas comunitarios en Bogotá",
                                      "status": "DELIVERED",
                                      "rejectionReason": null,
                                      "deliveryPoint": "Punto de Acopio StyleRadar: Calle 100 # 15-20, Usaquén, Bogotá",
                                      "slotStart": "2026-11-15T09:00:00",
                                      "slotEnd": "2026-11-15T12:00:00",
                                      "version": 3,
                                      "requestDate": "2026-11-11T10:00:00",
                                      "decisionDate": "2026-11-11T14:30:00",
                                      "receptionDate": "2026-11-15T11:45:00"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthenticated",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "UnauthorizedError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/unauthorized",
                                      "title": "No autenticado",
                                      "status": 401,
                                      "detail": "Token Bearer JWT ausente o inválido.",
                                      "instance": "/api/v1/donations/1/confirmation",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden: user does not represent the receiving foundation",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "NotReceivingFoundationError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/forbidden",
                                      "title": "Acceso denegado",
                                      "status": 403,
                                      "detail": "Acceso denegado: Solo la fundación receptora (ID: 501) puede confirmar la recepción física de la prenda.",
                                      "instance": "/api/v1/donations/1/confirmation",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Donation request not found",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "RequestNotFoundError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/not-found",
                                      "title": "Recurso no encontrado",
                                      "status": 404,
                                      "detail": "No existe solicitud de donación con ID: 999",
                                      "instance": "/api/v1/donations/999/confirmation",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict: request is not in IN_PICKUP state",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "InvalidStateForConfirmationError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/conflict",
                                      "title": "Conflicto de dominio",
                                      "status": 409,
                                      "detail": "La solicitud con ID: 1 se encuentra en estado APPROVED y no en IN_PICKUP para confirmar la entrega física.",
                                      "instance": "/api/v1/donations/1/confirmation",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "ConfirmationServerError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/internal-server-error",
                                      "title": "Error interno del servidor",
                                      "status": 500,
                                      "detail": "Error inesperado al confirmar la entrega física de la donación.",
                                      "instance": "/api/v1/donations/1/confirmation",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            )
    })
    @PatchMapping("/{id}/confirmation")
    @PreAuthorize("hasAnyRole('FOUNDATION', 'ADMIN')")
    public ResponseEntity<DonationRequestResponseDTO> confirmReceipt(
            @Parameter(description = "Donation request ID", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody ConfirmReceiptRequestDTO requestDTO) {
        resourceOwnershipValidator.validateFoundationOwnership(requestDTO.foundationId());
        DonationRequest updated = confirmDonationReceiptUseCase.confirmReceipt(requestDTO.foundationId(), id);
        return ResponseEntity.ok(donationRequestRestMapper.toResponse(updated));
    }

    @Operation(
            summary = "Get donor's donation history",
            description = "Lists all donation requests created by a specific donor user or partner store.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Donor's donation history",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = DonationRequestResponseDTO.class)),
                            examples = @ExampleObject(
                                    name = "DonorHistorySample",
                                    value = """
                                    [
                                      {
                                        "id": 1,
                                        "publicationId": 10,
                                        "donorId": 101,
                                        "foundationId": 501,
                                        "message": "Deseo donar esta chaqueta para los programas comunitarios en Bogotá",
                                        "status": "APPROVED",
                                        "rejectionReason": null,
                                        "deliveryPoint": "Sede Chapinero: Calle 67 # 11-45, Bogotá",
                                        "slotStart": "2026-11-15T09:00:00",
                                        "slotEnd": "2026-11-15T12:00:00",
                                        "version": 1,
                                        "requestDate": "2026-11-11T10:00:00",
                                        "decisionDate": "2026-11-11T14:30:00",
                                        "receptionDate": null
                                      }
                                    ]
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthenticated",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "UnauthorizedError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/unauthorized",
                                      "title": "No autenticado",
                                      "status": 401,
                                      "detail": "Token Bearer JWT ausente o inválido.",
                                      "instance": "/api/v1/donations/donors/101",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden: user is not the authenticated donor",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "NotDonorOwnerError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/forbidden",
                                      "title": "Acceso denegado",
                                      "status": 403,
                                      "detail": "El usuario autenticado (ID: 102) no coincide con el donante consultado (ID: 101).",
                                      "instance": "/api/v1/donations/donors/101",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "DonorHistoryServerError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/internal-server-error",
                                      "title": "Error interno del servidor",
                                      "status": 500,
                                      "detail": "Error inesperado al consultar el historial del donante.",
                                      "instance": "/api/v1/donations/donors/101",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            )
    })
    @GetMapping("/donors/{donorId}")
    @PreAuthorize("hasAnyRole('USER', 'STORE', 'FOUNDATION', 'ADMIN')")
    public ResponseEntity<List<DonationRequestResponseDTO>> getByDonorId(
            @Parameter(description = "Donor ID", example = "101")
            @PathVariable Long donorId) {
        resourceOwnershipValidator.validateOwnerOrAdmin(donorId);
        List<DonationRequest> list = createDonationRequestUseCase.getByDonorId(donorId);
        return ResponseEntity.ok(donationRequestRestMapper.toResponseList(list));
    }

    @Operation(
            summary = "Get foundation's donation request inbox",
            description = "Lists donation requests directed to an allied foundation with an optional status filter.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Foundation donation request inbox",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = DonationRequestResponseDTO.class)),
                            examples = @ExampleObject(
                                    name = "FoundationInboxSample",
                                    value = """
                                    [
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
                                        "requestDate": "2026-11-11T10:00:00",
                                        "decisionDate": null,
                                        "receptionDate": null
                                      }
                                    ]
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthenticated",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "UnauthorizedError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/unauthorized",
                                      "title": "No autenticado",
                                      "status": 401,
                                      "detail": "Token Bearer JWT ausente o inválido.",
                                      "instance": "/api/v1/donations/foundations/501/requests",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden: user does not represent the queried foundation",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "NotAuthenticatedFoundationError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/forbidden",
                                      "title": "Acceso denegado",
                                      "status": 403,
                                      "detail": "La fundación autenticada (ID: 502) no coincide con el foundationId consultado (ID: 501).",
                                      "instance": "/api/v1/donations/foundations/501/requests",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "FoundationInboxServerError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/internal-server-error",
                                      "title": "Error interno del servidor",
                                      "status": 500,
                                      "detail": "Error inesperado al consultar la bandeja de la fundación.",
                                      "instance": "/api/v1/donations/foundations/501/requests",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            )
    })
    @GetMapping("/foundations/{foundationId}/requests")
    @PreAuthorize("hasAnyRole('FOUNDATION', 'ADMIN')")
    public ResponseEntity<List<DonationRequestResponseDTO>> getByFoundationId(
            @Parameter(description = "Foundation ID", example = "501")
            @PathVariable Long foundationId,
            @Parameter(description = "Optional status filter", example = "PENDING")
            @RequestParam(required = false) DonationRequestStatus status) {
        resourceOwnershipValidator.validateFoundationOwnership(foundationId);
        List<DonationRequest> list = createDonationRequestUseCase.getByFoundationId(foundationId, status);
        return ResponseEntity.ok(donationRequestRestMapper.toResponseList(list));
    }
}
