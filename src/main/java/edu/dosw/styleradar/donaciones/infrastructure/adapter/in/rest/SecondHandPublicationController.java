package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.dosw.styleradar.donaciones.application.dto.request.CreatePublicationRequestDTO;
import edu.dosw.styleradar.donaciones.application.dto.request.EditPublicationRequestDTO;
import edu.dosw.styleradar.donaciones.application.dto.response.PublicationResponseDTO;
import edu.dosw.styleradar.donaciones.domain.model.ConservationStatus;
import edu.dosw.styleradar.donaciones.domain.model.GarmentType;
import edu.dosw.styleradar.donaciones.domain.model.LocalidadBogota;
import edu.dosw.styleradar.donaciones.domain.model.PublicationSearchCriteria;
import edu.dosw.styleradar.donaciones.domain.model.PublicationStatus;
import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;
import edu.dosw.styleradar.donaciones.domain.model.Size;
import edu.dosw.styleradar.donaciones.domain.model.Style;
import edu.dosw.styleradar.donaciones.domain.port.in.CreatePublicationCommand;
import edu.dosw.styleradar.donaciones.domain.port.in.CreatePublicationUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.EditPublicationCommand;
import edu.dosw.styleradar.donaciones.domain.port.in.GetPublicationsUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.ManagePublicationUseCase;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest.mapper.PublicationRestMapper;
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
 * Controlador REST para el catálogo de Economía Circular en StyleRadar.
 * Unifica prendas de Donación Benéfica (precio 0 COP, vitrina a fundaciones)
 * y de Segunda Mano C2C (precio > 0 COP con botón de chat al vendedor).
 * Restringido exclusivamente al territorio de las 20 localidades de Bogotá D.C.
 */
@RestController
@RequestMapping("/api/v1/second-hand/publications")
@RequiredArgsConstructor
@Tag(
        name = "Second-Hand & Circular Economy Publications",
        description = "Endpoints for managing C2C second-hand garments and charitable donations in Bogota D.C."
)
public class SecondHandPublicationController {

    private final CreatePublicationUseCase createPublicationUseCase;
    private final GetPublicationsUseCase getPublicationsUseCase;
    private final ManagePublicationUseCase managePublicationUseCase;
    private final PublicationRestMapper publicationRestMapper;
    private final ResourceOwnershipValidator resourceOwnershipValidator;

    @Operation(
            summary = "Create second-hand or donation publication",
            description = "Publishes a garment on StyleRadar for C2C resale (esDonacion=false, price > 0) or charitable donation (esDonacion=true, price forced to 0 COP). Unifies garment details, conservation status, and price in Bogota D.C. with up to 4 photos. Requires USER, STORE, or ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Publication successfully created in the showcase",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PublicationResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "SecondHandCreated",
                                    summary = "Sample created C2C second-hand publication",
                                    value = """
                                    {
                                      "id": 1,
                                      "userId": 101,
                                      "garmentName": "Chaqueta Denim Vintage",
                                      "description": "Chaqueta de mezclilla estilo retro de los 90s con botones metálicos originales",
                                      "garmentType": "Chaquetas",
                                      "brand": "Levi's",
                                      "color": "Azul",
                                      "style": "URBAN",
                                      "size": "M",
                                      "conservationStatus": "LIKE_NEW",
                                      "price": 75000.00,
                                      "esDonacion": false,
                                      "localidad": "CHAPINERO",
                                      "status": "AVAILABLE",
                                      "version": 0,
                                      "publicationDate": "2026-11-10T14:30:00",
                                      "updateDate": "2026-11-10T14:30:00",
                                      "photos": [
                                        "https://srdonacionesjmunar.blob.core.windows.net/donaciones-fotos/garment_chaqueta1.jpg",
                                        "https://srdonacionesjmunar.blob.core.windows.net/donaciones-fotos/garment_chaqueta2.jpg"
                                      ]
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request: invalid price, missing fields, or invalid Bogota district",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "ValidationError",
                                    summary = "District outside Bogota D.C. error",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/bad-request",
                                      "title": "Solicitud inválida",
                                      "status": 400,
                                      "detail": "La localidad de Bogotá es obligatoria. StyleRadar opera exclusivamente en las 20 localidades de Bogotá D.C.",
                                      "instance": "/api/v1/second-hand/publications",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthenticated: missing or expired JWT token",
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
                                      "detail": "Token JWT ausente, expirado o con firma no válida.",
                                      "instance": "/api/v1/second-hand/publications",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden: insufficient permissions or ownership mismatch",
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
                                      "detail": "Acceso denegado: El usuario autenticado no posee privilegios para publicar.",
                                      "instance": "/api/v1/second-hand/publications",
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
                                    name = "InternalServerError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/internal-server-error",
                                      "title": "Error interno del servidor",
                                      "status": 500,
                                      "detail": "Error inesperado al persistir la publicación.",
                                      "instance": "/api/v1/second-hand/publications",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            )
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'STORE', 'ADMIN')")
    public ResponseEntity<PublicationResponseDTO> createPublication(
            @Valid @RequestBody CreatePublicationRequestDTO requestDTO) {
        resourceOwnershipValidator.validateOwnerOrAdmin(requestDTO.userId());
        CreatePublicationCommand command = publicationRestMapper.toCommand(requestDTO);
        SecondHandPublication created = createPublicationUseCase.create(command);
        PublicationResponseDTO response = publicationRestMapper.toResponse(created);
        return ResponseEntity.created(URI.create("/api/v1/second-hand/publications/" + created.getId())).body(response);
    }

    @Operation(
            summary = "Explore and search garments in the general catalog",
            description = "Public query of the unified catalog (defaults to AVAILABLE status). Supports combining multiple filters: free text (search or searchTerm), garment category, color, size, conservation status, price range, brand, style, lifecycle status, donation flag (esDonacion), and Bogota D.C. district."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of matching garments retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = PublicationResponseDTO.class)),
                            examples = @ExampleObject(
                                    name = "CatalogListSample",
                                    summary = "Sample catalog listings in Chapinero and Usaquen",
                                    value = """
                                    [
                                      {
                                        "id": 1,
                                        "userId": 101,
                                        "garmentName": "Chaqueta Denim Vintage",
                                        "description": "Chaqueta de mezclilla estilo retro",
                                        "garmentType": "Chaquetas",
                                        "brand": "Levi's",
                                        "color": "Azul",
                                        "style": "URBAN",
                                        "size": "M",
                                        "conservationStatus": "LIKE_NEW",
                                        "price": 75000.00,
                                        "esDonacion": false,
                                        "localidad": "CHAPINERO",
                                        "status": "AVAILABLE",
                                        "version": 0,
                                        "publicationDate": "2026-11-10T14:30:00",
                                        "updateDate": "2026-11-10T14:30:00",
                                        "photos": ["https://srdonacionesjmunar.blob.core.windows.net/donaciones-fotos/garment_chaqueta1.jpg"]
                                      },
                                      {
                                        "id": 2,
                                        "userId": 102,
                                        "garmentName": "Abrigo Térmico de Invierno",
                                        "description": "Abrigo acolchado para donación a comunidad vulnerable",
                                        "garmentType": "Chaquetas",
                                        "brand": "Columbia",
                                        "color": "Negro",
                                        "style": "CASUAL",
                                        "size": "L",
                                        "conservationStatus": "GOOD_CONDITION",
                                        "price": 0.00,
                                        "esDonacion": true,
                                        "localidad": "USAQUEN",
                                        "status": "AVAILABLE",
                                        "version": 0,
                                        "publicationDate": "2026-11-10T15:00:00",
                                        "updateDate": "2026-11-10T15:00:00",
                                        "photos": ["https://srdonacionesjmunar.blob.core.windows.net/donaciones-fotos/garment_abrigo1.jpg"]
                                      }
                                    ]
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid filter parameters or types",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "FilterValidationError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/bad-request",
                                      "title": "Solicitud inválida",
                                      "status": 400,
                                      "detail": "Parámetro 'localidad' no válido. Solo se admiten las 20 localidades de Bogotá D.C.",
                                      "instance": "/api/v1/second-hand/publications",
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
                                    name = "SearchServerError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/internal-server-error",
                                      "title": "Error interno del servidor",
                                      "status": 500,
                                      "detail": "Error inesperado al consultar el catálogo de publicaciones.",
                                      "instance": "/api/v1/second-hand/publications",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            )
    })
    @GetMapping
    public ResponseEntity<List<PublicationResponseDTO>> searchPublications(
            @Parameter(description = "Free-text search term matching garment name, brand, or description", example = "chaqueta")
            @RequestParam(required = false) String searchTerm,
            @Parameter(description = "Alias for free-text search parameter", example = "chaqueta")
            @RequestParam(required = false) String search,
            @Parameter(description = "Garment category filter", example = "Chaquetas")
            @RequestParam(required = false) GarmentType type,
            @Parameter(description = "Optional alias for garment category filter", example = "Chaquetas")
            @RequestParam(required = false) GarmentType garmentType,
            @Parameter(description = "Predominant color filter", example = "Azul")
            @RequestParam(required = false) String color,
            @Parameter(description = "Standardized size filter", example = "M")
            @RequestParam(required = false) Size size,
            @Parameter(description = "Conservation status filter", example = "LIKE_NEW")
            @RequestParam(required = false) ConservationStatus conservationStatus,
            @Parameter(description = "Minimum price in COP", example = "20000.00")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price in COP", example = "100000.00")
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Brand name filter", example = "Levi's")
            @RequestParam(required = false) String brand,
            @Parameter(description = "Fashion style filter", example = "URBAN")
            @RequestParam(required = false) Style style,
            @Parameter(description = "Publication lifecycle status filter (defaults to AVAILABLE)", example = "AVAILABLE")
            @RequestParam(required = false) PublicationStatus status,
            @Parameter(description = "Circular economy filter: true for charitable donations, false for C2C second-hand resale", example = "false")
            @RequestParam(required = false) Boolean esDonacion,
            @Parameter(description = "Bogota D.C. district (localidad) filter", example = "CHAPINERO")
            @RequestParam(required = false) LocalidadBogota localidad) {

        String effectiveSearch = (searchTerm != null && !searchTerm.isBlank()) ? searchTerm : search;
        PublicationSearchCriteria criteria = PublicationSearchCriteria.builder()
                .searchTerm(effectiveSearch)
                .type(type != null ? type : garmentType)
                .color(color)
                .size(size)
                .conservationStatus(conservationStatus)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .brand(brand)
                .style(style)
                .status(status)
                .esDonacion(esDonacion)
                .localidad(localidad)
                .build();

        List<SecondHandPublication> results = getPublicationsUseCase.search(criteria);
        return ResponseEntity.ok(publicationRestMapper.toResponseList(results));
    }

    @Operation(
            summary = "Get publication details by ID",
            description = "Retrieves individual garment metadata including details, size, status, photos, owner user ID, price, and Bogota D.C. district."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Publication found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PublicationResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "PublicationDetailsSample",
                                    value = """
                                    {
                                      "id": 10,
                                      "userId": 101,
                                      "garmentName": "Chaqueta Denim Vintage",
                                      "description": "Chaqueta denim estilo retro de los 90s con botones originales",
                                      "garmentType": "Chaquetas",
                                      "brand": "Levi's",
                                      "color": "Azul",
                                      "style": "URBAN",
                                      "size": "M",
                                      "conservationStatus": "LIKE_NEW",
                                      "price": 75000.00,
                                      "esDonacion": false,
                                      "localidad": "CHAPINERO",
                                      "status": "AVAILABLE",
                                      "version": 0,
                                      "publicationDate": "2026-11-10T14:30:00",
                                      "updateDate": "2026-11-10T14:30:00",
                                      "photos": ["https://srdonacionesjmunar.blob.core.windows.net/donaciones-fotos/garment_chaqueta1.jpg"]
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
                                    name = "PublicationNotFound",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/not-found",
                                      "title": "Recurso no encontrado",
                                      "status": 404,
                                      "detail": "No existe publicación con ID: 999",
                                      "instance": "/api/v1/second-hand/publications/999",
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
                                    name = "DetailServerError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/internal-server-error",
                                      "title": "Error interno del servidor",
                                      "status": 500,
                                      "detail": "Error inesperado al buscar la publicación por ID.",
                                      "instance": "/api/v1/second-hand/publications/10",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<PublicationResponseDTO> getPublicationById(
            @Parameter(description = "Unique publication identifier", example = "10")
            @PathVariable Long id) {
        SecondHandPublication publication = getPublicationsUseCase.getById(id);
        return ResponseEntity.ok(publicationRestMapper.toResponse(publication));
    }

    @Operation(
            summary = "List user's own publications",
            description = "Retrieves all second-hand and donation listings created by a specific user across all states. Requires authentication as the owner user or ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of user's publications",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = PublicationResponseDTO.class)),
                            examples = @ExampleObject(
                                    name = "UserPublicationsSample",
                                    value = """
                                    [
                                      {
                                        "id": 10,
                                        "userId": 101,
                                        "garmentName": "Chaqueta Denim Vintage",
                                        "description": "Chaqueta denim estilo retro de los 90s",
                                        "garmentType": "Chaquetas",
                                        "brand": "Levi's",
                                        "color": "Azul",
                                        "style": "URBAN",
                                        "size": "M",
                                        "conservationStatus": "LIKE_NEW",
                                        "price": 75000.00,
                                        "esDonacion": false,
                                        "localidad": "CHAPINERO",
                                        "status": "AVAILABLE",
                                        "version": 0,
                                        "publicationDate": "2026-11-10T14:30:00",
                                        "updateDate": "2026-11-10T14:30:00",
                                        "photos": ["https://srdonacionesjmunar.blob.core.windows.net/donaciones-fotos/garment_chaqueta1.jpg"]
                                      }
                                    ]
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
                                      "detail": "Token Bearer JWT ausente o inválido.",
                                      "instance": "/api/v1/second-hand/publications/users/101",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden: user is not the owner nor an administrator",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "NotOwnerError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/forbidden",
                                      "title": "Acceso denegado",
                                      "status": 403,
                                      "detail": "El usuario autenticado (ID: 102) no tiene autorización para operar sobre publicaciones del usuario ID: 101.",
                                      "instance": "/api/v1/second-hand/publications/users/101",
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
                                    name = "UserPublicationsServerError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/internal-server-error",
                                      "title": "Error interno del servidor",
                                      "status": 500,
                                      "detail": "Error inesperado al consultar publicaciones por usuario.",
                                      "instance": "/api/v1/second-hand/publications/users/101",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            )
    })
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'STORE', 'FOUNDATION', 'ADMIN')")
    public ResponseEntity<List<PublicationResponseDTO>> getPublicationsByUserId(
            @Parameter(description = "Owner user ID", example = "101")
            @PathVariable Long userId) {
        resourceOwnershipValidator.validateOwnerOrAdmin(userId);
        List<SecondHandPublication> list = getPublicationsUseCase.getByUserId(userId);
        return ResponseEntity.ok(publicationRestMapper.toResponseList(list));
    }

    @Operation(
            summary = "Edit second-hand publication",
            description = "Allows the owner to update garment metadata, photos, price, or district when the publication is in an editable state (blocks SOLD, RETIRED, IN_DONATION, DONATED). Requires USER, STORE, or ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Publication updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PublicationResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "PublicationUpdatedSample",
                                    value = """
                                    {
                                      "id": 10,
                                      "userId": 101,
                                      "garmentName": "Chaqueta Denim Vintage Premium",
                                      "description": "Chaqueta de mezclilla estilo retro de los 90s con botones originales",
                                      "garmentType": "Chaquetas",
                                      "brand": "Levi's",
                                      "color": "Azul Claro",
                                      "style": "URBAN",
                                      "size": "M",
                                      "conservationStatus": "LIKE_NEW",
                                      "price": 68000.00,
                                      "esDonacion": false,
                                      "localidad": "CHAPINERO",
                                      "status": "AVAILABLE",
                                      "version": 1,
                                      "publicationDate": "2026-11-10T14:30:00",
                                      "updateDate": "2026-11-11T12:00:00",
                                      "photos": ["https://srdonacionesjmunar.blob.core.windows.net/donaciones-fotos/garment_chaqueta1.jpg"]
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
                                    name = "InvalidEditInput",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/bad-request",
                                      "title": "Solicitud inválida",
                                      "status": 400,
                                      "detail": "El precio para venta C2C debe ser superior a 0 COP.",
                                      "instance": "/api/v1/second-hand/publications/10",
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
                                      "instance": "/api/v1/second-hand/publications/10",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden: user is not the publication owner",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "NotOwnerEdit",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/forbidden",
                                      "title": "Acceso denegado",
                                      "status": 403,
                                      "detail": "Acceso denegado: El usuario autenticado (ID: 102) no es propietario de la publicación ID: 10.",
                                      "instance": "/api/v1/second-hand/publications/10",
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
                                    name = "PublicationNotFoundEdit",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/not-found",
                                      "title": "Recurso no encontrado",
                                      "status": 404,
                                      "detail": "No existe publicación con ID: 999",
                                      "instance": "/api/v1/second-hand/publications/999",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Blocking state conflict: publication already sold, retired, in donation, or donated",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "EditConflict",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/conflict",
                                      "title": "Conflicto de dominio",
                                      "status": 409,
                                      "detail": "No se puede editar una publicación que ya ha sido vendida",
                                      "instance": "/api/v1/second-hand/publications/10",
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
                                    name = "EditServerError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/internal-server-error",
                                      "title": "Error interno del servidor",
                                      "status": 500,
                                      "detail": "Error inesperado al actualizar la publicación.",
                                      "instance": "/api/v1/second-hand/publications/10",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            )
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'STORE', 'ADMIN')")
    public ResponseEntity<PublicationResponseDTO> editPublication(
            @Parameter(description = "ID of the publication to edit", example = "10")
            @PathVariable Long id,
            @Valid @RequestBody EditPublicationRequestDTO requestDTO) {
        resourceOwnershipValidator.validateOwnerOrAdmin(requestDTO.userId());
        EditPublicationCommand command = publicationRestMapper.toCommand(id, requestDTO);
        SecondHandPublication updated = managePublicationUseCase.edit(command);
        return ResponseEntity.ok(publicationRestMapper.toResponse(updated));
    }

    @Operation(
            summary = "Mark second-hand garment as SOLD",
            description = "Transitions the publication status to SOLD after finalizing the C2C sale agreed via chat. Publishes the RabbitMQ integration event toward ms-chat and ms-notificaciones with the mandatory compradorId. Requires owner authorization.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Garment marked as sold and RabbitMQ event published",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PublicationResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "GarmentSoldSample",
                                    value = """
                                    {
                                      "id": 10,
                                      "userId": 101,
                                      "garmentName": "Chaqueta Denim Vintage",
                                      "description": "Chaqueta denim estilo retro de los 90s",
                                      "garmentType": "Chaquetas",
                                      "brand": "Levi's",
                                      "color": "Azul",
                                      "style": "URBAN",
                                      "size": "M",
                                      "conservationStatus": "LIKE_NEW",
                                      "price": 75000.00,
                                      "esDonacion": false,
                                      "localidad": "CHAPINERO",
                                      "status": "SOLD",
                                      "version": 1,
                                      "publicationDate": "2026-11-10T14:30:00",
                                      "updateDate": "2026-11-11T16:00:00",
                                      "photos": ["https://srdonacionesjmunar.blob.core.windows.net/donaciones-fotos/garment_chaqueta1.jpg"]
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing mandatory parameter (compradorId)",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "MissingBuyerId",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/bad-request",
                                      "title": "Solicitud inválida",
                                      "status": 400,
                                      "detail": "El parámetro 'compradorId' es obligatorio para cerrar la venta C2C y coordinar la conversación con ms-chat.",
                                      "instance": "/api/v1/second-hand/publications/10/sale",
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
                                      "detail": "Token Bearer JWT ausente o inválido.",
                                      "instance": "/api/v1/second-hand/publications/10/sale",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden: user is not the owner seller",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "NotSellerOwner",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/forbidden",
                                      "title": "Acceso denegado",
                                      "status": 403,
                                      "detail": "Acceso denegado: El usuario autenticado (ID: 102) no es el vendedor de la publicación ID: 10.",
                                      "instance": "/api/v1/second-hand/publications/10/sale",
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
                                    name = "PublicationNotFoundSale",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/not-found",
                                      "title": "Recurso no encontrado",
                                      "status": 404,
                                      "detail": "No existe publicación con ID: 999",
                                      "instance": "/api/v1/second-hand/publications/999/sale",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict: publication already sold, retired, or in donation workflow",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "SaleConflict",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/conflict",
                                      "title": "Conflicto de dominio",
                                      "status": 409,
                                      "detail": "La publicación ya se encuentra marcada como vendida o no está disponible para reventa C2C.",
                                      "instance": "/api/v1/second-hand/publications/10/sale",
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
                                    name = "SaleServerError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/internal-server-error",
                                      "title": "Error interno del servidor",
                                      "status": 500,
                                      "detail": "Error inesperado al registrar la venta y publicar el evento a RabbitMQ.",
                                      "instance": "/api/v1/second-hand/publications/10/sale",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            )
    })
    @PatchMapping("/{id}/sale")
    @PreAuthorize("hasAnyRole('USER', 'STORE', 'ADMIN')")
    public ResponseEntity<PublicationResponseDTO> markAsSold(
            @Parameter(description = "ID of the sold publication", example = "10")
            @PathVariable Long id,
            @Parameter(description = "ID of the authenticated seller user", example = "101")
            @RequestParam Long userId,
            @Parameter(description = "ID of the buyer in the C2C chat (mandatory to coordinate chat closure)", example = "102", required = true)
            @RequestParam Long compradorId) {
        resourceOwnershipValidator.validateOwnerOrAdmin(userId);
        SecondHandPublication sold = managePublicationUseCase.markAsSold(userId, id, compradorId);
        return ResponseEntity.ok(publicationRestMapper.toResponse(sold));
    }

    @Operation(
            summary = "Retire publication from catalog",
            description = "Logically retires the owner's garment from the public catalog (RETIRED status), blocking listings already sold, in donation, or donated. Requires USER, STORE, or ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Publication retired successfully"),
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
                                      "instance": "/api/v1/second-hand/publications/10",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden: user does not own the publication",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "NotOwnerRetire",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/forbidden",
                                      "title": "Acceso denegado",
                                      "status": 403,
                                      "detail": "Acceso denegado: El usuario autenticado (ID: 102) no es propietario de la publicación ID: 10.",
                                      "instance": "/api/v1/second-hand/publications/10",
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
                                    name = "PublicationNotFoundRetire",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/not-found",
                                      "title": "Recurso no encontrado",
                                      "status": 404,
                                      "detail": "No existe publicación con ID: 999",
                                      "instance": "/api/v1/second-hand/publications/999",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict: cannot retire an already sold, in-donation, or donated garment",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "RetireStateConflict",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/conflict",
                                      "title": "Conflicto de dominio",
                                      "status": 409,
                                      "detail": "No se puede retirar una prenda que ya ha sido donada a una fundación.",
                                      "instance": "/api/v1/second-hand/publications/10",
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
                                    name = "RetireServerError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/internal-server-error",
                                      "title": "Error interno del servidor",
                                      "status": 500,
                                      "detail": "Error inesperado al retirar la publicación de la vitrina.",
                                      "instance": "/api/v1/second-hand/publications/10",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            )
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'STORE', 'ADMIN')")
    public ResponseEntity<Void> retirePublication(
            @Parameter(description = "ID of the publication to retire", example = "10")
            @PathVariable Long id,
            @Parameter(description = "ID of the owner user", example = "101")
            @RequestParam Long userId) {
        resourceOwnershipValidator.validateOwnerOrAdmin(userId);
        managePublicationUseCase.retire(userId, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Convert existing publication to donation",
            description = "Transitions an available second-hand listing to IN_DONATION status with price set to 0 COP so it can be donated to a partner foundation. Requires owner authorization.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Garment successfully moved to the charitable donation channel",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PublicationResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "GarmentMovedToDonation",
                                    value = """
                                    {
                                      "id": 10,
                                      "userId": 101,
                                      "garmentName": "Chaqueta Denim Vintage",
                                      "description": "Chaqueta denim estilo retro para donación",
                                      "garmentType": "Chaquetas",
                                      "brand": "Levi's",
                                      "color": "Azul",
                                      "style": "URBAN",
                                      "size": "M",
                                      "conservationStatus": "LIKE_NEW",
                                      "price": 0.00,
                                      "esDonacion": true,
                                      "localidad": "CHAPINERO",
                                      "status": "IN_DONATION",
                                      "version": 1,
                                      "publicationDate": "2026-11-10T14:30:00",
                                      "updateDate": "2026-11-11T16:00:00",
                                      "photos": ["https://srdonacionesjmunar.blob.core.windows.net/donaciones-fotos/garment_chaqueta1.jpg"]
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
                                      "instance": "/api/v1/second-hand/publications/10/donation",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden: user does not own the garment",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "NotOwnerDonation",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/forbidden",
                                      "title": "Acceso denegado",
                                      "status": 403,
                                      "detail": "Acceso denegado: El usuario autenticado (ID: 102) no es propietario de la prenda ID: 10.",
                                      "instance": "/api/v1/second-hand/publications/10/donation",
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
                                    name = "PublicationNotFoundDonation",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/not-found",
                                      "title": "Recurso no encontrado",
                                      "status": 404,
                                      "detail": "No existe publicación con ID: 999",
                                      "instance": "/api/v1/second-hand/publications/999/donation",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Publication is not available for donation conversion",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "NotAvailableForDonation",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/conflict",
                                      "title": "Conflicto de dominio",
                                      "status": 409,
                                      "detail": "La publicación no se encuentra en estado AVAILABLE para ser transferida al canal benéfico.",
                                      "instance": "/api/v1/second-hand/publications/10/donation",
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
                                    name = "DonationConversionServerError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/internal-server-error",
                                      "title": "Error interno del servidor",
                                      "status": 500,
                                      "detail": "Error inesperado al destinar la prenda al canal de donación.",
                                      "instance": "/api/v1/second-hand/publications/10/donation",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            )
    })
    @PatchMapping("/{id}/donation")
    @PreAuthorize("hasAnyRole('USER', 'STORE', 'ADMIN')")
    public ResponseEntity<PublicationResponseDTO> markForDonation(
            @Parameter(description = "ID of the publication to convert", example = "10")
            @PathVariable Long id,
            @Parameter(description = "ID of the owner donor user", example = "101")
            @RequestParam Long userId) {
        resourceOwnershipValidator.validateOwnerOrAdmin(userId);
        SecondHandPublication updated = managePublicationUseCase.markForDonation(userId, id);
        return ResponseEntity.ok(publicationRestMapper.toResponse(updated));
    }
}
