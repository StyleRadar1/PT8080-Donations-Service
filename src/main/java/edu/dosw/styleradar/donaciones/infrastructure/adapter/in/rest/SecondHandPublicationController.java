package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/segunda-mano")
@RequiredArgsConstructor
@Tag(
        name = "Publicaciones - Economía Circular",
        description = "Endpoints para la gestión de prendas de Segunda Mano C2C y Donaciones Benéficas en Bogotá D.C. (SR-RF-33 a SR-RF-42)"
)
public class SecondHandPublicationController {

    private final CreatePublicationUseCase createPublicationUseCase;
    private final GetPublicationsUseCase getPublicationsUseCase;
    private final ManagePublicationUseCase managePublicationUseCase;
    private final PublicationRestMapper publicationRestMapper;
    private final ResourceOwnershipValidator resourceOwnershipValidator;

    @Operation(
            summary = "Crear publicación (Segunda Mano o Donación)",
            description = "Publica una prenda en la plataforma StyleRadar para reventa C2C (esDonacion=false, precio > 0) o para Donación Benéfica (esDonacion=true, precio forzoso 0 COP). Requiere ubicación en una de las 20 localidades oficiales de Bogotá D.C. y hasta 4 fotografías. Requiere rol USER, STORE, FOUNDATION o ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Publicación creada exitosamente en la vitrina",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PublicationResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "PrendaSegundaManoCreada",
                                    summary = "Ejemplo de publicación de segunda mano C2C creada",
                                    value = """
                                    {
                                      "id": 1,
                                      "userId": 101,
                                      "garmentName": "Chaqueta Denim Vintage",
                                      "description": "Chaqueta de mezclilla estilo retro de los 90s con botones metálicos originales",
                                      "garmentType": "UPPER",
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
                    description = "Solicitud inválida: precio inválido o localidad fuera de Bogotá D.C.",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "ErrorValidacionBogota",
                                    summary = "Error de localidad no perteneciente a Bogotá",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/bad-request",
                                      "title": "Solicitud inválida",
                                      "status": 400,
                                      "detail": "La localidad de Bogotá es obligatoria. StyleRadar opera exclusivamente en las 20 localidades de Bogotá D.C.",
                                      "instance": "/api/v1/segunda-mano",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado: token JWT ausente o expirado",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "Error401",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/unauthorized",
                                      "title": "No autenticado",
                                      "status": 401,
                                      "detail": "Token JWT ausente, expirado o con firma no válida.",
                                      "instance": "/api/v1/segunda-mano",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado: permisos insuficientes",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "Error403",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/forbidden",
                                      "title": "Acceso denegado",
                                      "status": 403,
                                      "detail": "Acceso denegado: El usuario autenticado no posee privilegios para publicar.",
                                      "instance": "/api/v1/segunda-mano",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "Error500",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/internal-server-error",
                                      "title": "Error interno del servidor",
                                      "status": 500,
                                      "detail": "Error inesperado al persistir la publicación.",
                                      "instance": "/api/v1/segunda-mano",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            )
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'STORE', 'FOUNDATION', 'ADMIN')")
    public ResponseEntity<PublicationResponseDTO> createPublication(
            @Valid @RequestBody CreatePublicationRequestDTO requestDTO) {
        resourceOwnershipValidator.validateOwnerOrAdmin(requestDTO.userId());
        CreatePublicationCommand command = publicationRestMapper.toCommand(requestDTO);
        SecondHandPublication created = createPublicationUseCase.create(command);
        PublicationResponseDTO response = publicationRestMapper.toResponse(created);
        return ResponseEntity.created(URI.create("/api/v1/segunda-mano/" + created.getId())).body(response);
    }

    @Operation(
            summary = "Buscar y filtrar publicaciones en catálogo de Bogotá D.C.",
            description = "Consulta pública del catálogo unificado. Permite combinar múltiples filtros: texto libre, tipo de prenda, color, talla, estado de conservación, rango de precio, marca, estilo, estado de publicación, filtro de donación (esDonacion=true para Vitrina Abierta benéfica; esDonacion=false para Segunda Mano C2C), y localidad oficial de Bogotá D.C."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de prendas encontradas según los filtros aplicados",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = PublicationResponseDTO.class)),
                            examples = @ExampleObject(
                                    name = "ListadoPrendasVitrina",
                                    summary = "Ejemplo de catálogo de prendas encontradas en Chapinero",
                                    value = """
                                    [
                                      {
                                        "id": 1,
                                        "userId": 101,
                                        "garmentName": "Chaqueta Denim Vintage",
                                        "description": "Chaqueta de mezclilla estilo retro",
                                        "garmentType": "UPPER",
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
                                        "garmentType": "UPPER",
                                        "brand": "Columbia",
                                        "color": "Negro",
                                        "style": "CASUAL",
                                        "size": "L",
                                        "conservationStatus": "GOOD",
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
                    description = "Filtros con formato o tipos no válidos",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "ErrorFiltros",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/bad-request",
                                      "title": "Solicitud inválida",
                                      "status": 400,
                                      "detail": "Parámetro 'localidad' no válido. Solo se admiten las 20 localidades de Bogotá D.C.",
                                      "instance": "/api/v1/segunda-mano",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "Error500Busqueda",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/internal-server-error",
                                      "title": "Error interno del servidor",
                                      "status": 500,
                                      "detail": "Error inesperado al consultar el catálogo de publicaciones.",
                                      "instance": "/api/v1/segunda-mano",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            )
    })
    @GetMapping
    public ResponseEntity<List<PublicationResponseDTO>> searchPublications(
            @Parameter(description = "Término textual para buscar en nombre, marca y descripción", example = "chaqueta")
            @RequestParam(required = false) String searchTerm,
            @Parameter(description = "Categoría de prenda textil", example = "UPPER")
            @RequestParam(required = false) GarmentType type,
            @Parameter(description = "Color predominante", example = "Azul")
            @RequestParam(required = false) String color,
            @Parameter(description = "Talla estandarizada", example = "M")
            @RequestParam(required = false) Size size,
            @Parameter(description = "Estado de conservación", example = "LIKE_NEW")
            @RequestParam(required = false) ConservationStatus conservationStatus,
            @Parameter(description = "Precio mínimo en COP", example = "20000.00")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Precio máximo en COP", example = "100000.00")
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Marca fabricante", example = "Levi's")
            @RequestParam(required = false) String brand,
            @Parameter(description = "Estilo clasificado", example = "URBAN")
            @RequestParam(required = false) Style style,
            @Parameter(description = "Estado en el ciclo de vida", example = "AVAILABLE")
            @RequestParam(required = false) PublicationStatus status,
            @Parameter(description = "Filtro de economía circular: TRUE para donaciones benéficas; FALSE para reventa segunda mano", example = "false")
            @RequestParam(required = false) Boolean esDonacion,
            @Parameter(description = "Localidad de Bogotá D.C.", example = "CHAPINERO")
            @RequestParam(required = false) LocalidadBogota localidad) {

        PublicationSearchCriteria criteria = PublicationSearchCriteria.builder()
                .searchTerm(searchTerm)
                .type(type)
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
            summary = "Obtener detalle de publicación por ID",
            description = "Consulta individual de una prenda con todos sus metadatos (prenda, talla, estado, fotos, vendedor, precio y localidad en Bogotá)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Publicación encontrada",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PublicationResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "DetallePrenda",
                                    value = """
                                    {
                                      "id": 10,
                                      "userId": 101,
                                      "garmentName": "Chaqueta Denim Vintage",
                                      "description": "Chaqueta denim estilo retro de los 90s con botones originales",
                                      "garmentType": "UPPER",
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
                    description = "Publicación no encontrada",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "PublicacionNoEncontrada",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/not-found",
                                      "title": "Recurso no encontrado",
                                      "status": 404,
                                      "detail": "No existe publicación con ID: 999",
                                      "instance": "/api/v1/segunda-mano/999",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "Error500Detalle",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/internal-server-error",
                                      "title": "Error interno del servidor",
                                      "status": 500,
                                      "detail": "Error inesperado al buscar la publicación por ID.",
                                      "instance": "/api/v1/segunda-mano/10",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<PublicationResponseDTO> getPublicationById(
            @Parameter(description = "Identificador único de la publicación", example = "10")
            @PathVariable Long id) {
        SecondHandPublication publication = getPublicationsUseCase.getById(id);
        return ResponseEntity.ok(publicationRestMapper.toResponse(publication));
    }

    @Operation(
            summary = "Listar publicaciones por usuario",
            description = "Consulta el historial de prendas publicadas por un usuario o entidad en el módulo de economía circular. Requiere autenticación y propiedad del usuario o rol ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de publicaciones del usuario",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = PublicationResponseDTO.class)),
                            examples = @ExampleObject(
                                    name = "PublicacionesUsuario",
                                    value = """
                                    [
                                      {
                                        "id": 10,
                                        "userId": 101,
                                        "garmentName": "Chaqueta Denim Vintage",
                                        "description": "Chaqueta denim estilo retro de los 90s",
                                        "garmentType": "UPPER",
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
                    description = "No autenticado",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "NoAutenticado",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/unauthorized",
                                      "title": "No autenticado",
                                      "status": 401,
                                      "detail": "Token Bearer JWT ausente o inválido.",
                                      "instance": "/api/v1/segunda-mano/user/101",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado: no es propietario ni ADMIN",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "NoEsPropietario",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/forbidden",
                                      "title": "Acceso denegado",
                                      "status": 403,
                                      "detail": "El usuario autenticado (ID: 102) no tiene autorización para operar sobre publicaciones del usuario ID: 101.",
                                      "instance": "/api/v1/segunda-mano/user/101",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "Error500Usuario",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/internal-server-error",
                                      "title": "Error interno del servidor",
                                      "status": 500,
                                      "detail": "Error inesperado al consultar publicaciones por usuario.",
                                      "instance": "/api/v1/segunda-mano/user/101",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            )
    })
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'STORE', 'FOUNDATION', 'ADMIN')")
    public ResponseEntity<List<PublicationResponseDTO>> getPublicationsByUserId(
            @Parameter(description = "ID del usuario propietario", example = "101")
            @PathVariable Long userId) {
        resourceOwnershipValidator.validateOwnerOrAdmin(userId);
        List<SecondHandPublication> list = getPublicationsUseCase.getByUserId(userId);
        return ResponseEntity.ok(publicationRestMapper.toResponseList(list));
    }

    @Operation(
            summary = "Editar publicación",
            description = "Permite al propietario actualizar datos de la prenda, fotografías, precio o localidad siempre que la publicación se encuentre en estado editable (no vendida, ni retirada, ni donada). Requiere rol USER, STORE, FOUNDATION o ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Publicación actualizada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PublicationResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "PublicacionActualizada",
                                    value = """
                                    {
                                      "id": 10,
                                      "userId": 101,
                                      "garmentName": "Chaqueta Denim Vintage Premium",
                                      "description": "Chaqueta de mezclilla estilo retro de los 90s con botones originales",
                                      "garmentType": "UPPER",
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
                    description = "Datos de entrada inválidos",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "DatosEdicionInvalidos",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/bad-request",
                                      "title": "Solicitud inválida",
                                      "status": 400,
                                      "detail": "El precio para venta C2C debe ser superior a 0 COP.",
                                      "instance": "/api/v1/segunda-mano/10",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "NoAutenticado",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/unauthorized",
                                      "title": "No autenticado",
                                      "status": 401,
                                      "detail": "Token Bearer JWT ausente o inválido.",
                                      "instance": "/api/v1/segunda-mano/10",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "No es propietario de la publicación",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "NoEsPropietarioEdicion",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/forbidden",
                                      "title": "Acceso denegado",
                                      "status": 403,
                                      "detail": "Acceso denegado: El usuario autenticado (ID: 102) no es propietario de la publicación ID: 10.",
                                      "instance": "/api/v1/segunda-mano/10",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Publicación no encontrada",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "PublicacionNoEncontradaEdicion",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/not-found",
                                      "title": "Recurso no encontrado",
                                      "status": 404,
                                      "detail": "No existe publicación con ID: 999",
                                      "instance": "/api/v1/segunda-mano/999",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Estado bloqueante: publicación ya vendida, retirada o donada",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "ConflictoEdicion",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/conflict",
                                      "title": "Conflicto de dominio",
                                      "status": 409,
                                      "detail": "No se puede editar una publicación que ya ha sido vendida",
                                      "instance": "/api/v1/segunda-mano/10",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "Error500Edicion",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/internal-server-error",
                                      "title": "Error interno del servidor",
                                      "status": 500,
                                      "detail": "Error inesperado al actualizar la publicación.",
                                      "instance": "/api/v1/segunda-mano/10",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            )
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'STORE', 'FOUNDATION', 'ADMIN')")
    public ResponseEntity<PublicationResponseDTO> editPublication(
            @Parameter(description = "ID de la publicación a editar", example = "10")
            @PathVariable Long id,
            @Valid @RequestBody EditPublicationRequestDTO requestDTO) {
        resourceOwnershipValidator.validateOwnerOrAdmin(requestDTO.userId());
        EditPublicationCommand command = publicationRestMapper.toCommand(id, requestDTO);
        SecondHandPublication updated = managePublicationUseCase.edit(command);
        return ResponseEntity.ok(publicationRestMapper.toResponse(updated));
    }

    @Operation(
            summary = "Marcar prenda de segunda mano como VENDIDA (C2C)",
            description = "Transiciona el estado de la publicación a SOLD al concretarse la venta pactada por chat. Publica el evento de integración RabbitMQ hacia ms-chat (exchange styleradar.events, routing key segunda-mano.prenda.vendida) con compradorId para preservar la conversación con el comprador y cerrar las demás. Requiere ser el vendedor propietario.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Prenda marcada como vendida y evento RabbitMQ publicado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PublicationResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "PrendaVendidaEjemplo",
                                    value = """
                                    {
                                      "id": 10,
                                      "userId": 101,
                                      "garmentName": "Chaqueta Denim Vintage",
                                      "description": "Chaqueta denim estilo retro de los 90s",
                                      "garmentType": "UPPER",
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
                    description = "Parámetro obligatorio faltante (compradorId)",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "FaltaCompradorId",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/bad-request",
                                      "title": "Solicitud inválida",
                                      "status": 400,
                                      "detail": "El parámetro 'compradorId' es obligatorio para cerrar la venta C2C y coordinar la conversación con ms-chat.",
                                      "instance": "/api/v1/segunda-mano/10/sell",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token JWT inválido",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "NoAutenticado",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/unauthorized",
                                      "title": "No autenticado",
                                      "status": 401,
                                      "detail": "Token Bearer JWT ausente o inválido.",
                                      "instance": "/api/v1/segunda-mano/10/sell",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado: no es el vendedor propietario",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "NoEsVendedorPropietario",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/forbidden",
                                      "title": "Acceso denegado",
                                      "status": 403,
                                      "detail": "Acceso denegado: El usuario autenticado (ID: 102) no es el vendedor de la publicación ID: 10.",
                                      "instance": "/api/v1/segunda-mano/10/sell",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Publicación no encontrada",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "PublicacionNoEncontradaVenta",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/not-found",
                                      "title": "Recurso no encontrado",
                                      "status": 404,
                                      "detail": "No existe publicación con ID: 999",
                                      "instance": "/api/v1/segunda-mano/999/sell",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflicto: la publicación ya fue vendida, retirada o está en canal de donación",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "ConflictoVenta",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/conflict",
                                      "title": "Conflicto de dominio",
                                      "status": 409,
                                      "detail": "La publicación ya se encuentra marcada como vendida o no está disponible para reventa C2C.",
                                      "instance": "/api/v1/segunda-mano/10/sell",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "ErrorServidorVenta",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/internal-server-error",
                                      "title": "Error interno del servidor",
                                      "status": 500,
                                      "detail": "Error inesperado al registrar la venta y publicar el evento a RabbitMQ.",
                                      "instance": "/api/v1/segunda-mano/10/sell",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            )
    })
    @PatchMapping("/{id}/sell")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PublicationResponseDTO> markAsSold(
            @Parameter(description = "ID de la publicación vendida", example = "10")
            @PathVariable Long id,
            @Parameter(description = "ID del usuario vendedor autenticado", example = "101")
            @RequestParam Long userId,
            @Parameter(description = "ID del comprador en el chat C2C (obligatorio para preservar la conversación en ms-chat)", example = "102", required = true)
            @RequestParam Long compradorId) {
        resourceOwnershipValidator.validateOwnerOrAdmin(userId);
        SecondHandPublication sold = managePublicationUseCase.markAsSold(userId, id, compradorId);
        return ResponseEntity.ok(publicationRestMapper.toResponse(sold));
    }

    @Operation(
            summary = "Retirar publicación de la vitrina",
            description = "El usuario retira su prenda del catálogo público (estado RETIRED). Requiere rol USER, STORE o ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Publicación retirada exitosamente"),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "NoAutenticado",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/unauthorized",
                                      "title": "No autenticado",
                                      "status": 401,
                                      "detail": "Token Bearer JWT ausente o inválido.",
                                      "instance": "/api/v1/segunda-mano/10/retire",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "No es propietario de la prenda",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "NoEsPropietarioRetiro",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/forbidden",
                                      "title": "Acceso denegado",
                                      "status": 403,
                                      "detail": "Acceso denegado: El usuario autenticado (ID: 102) no es propietario de la publicación ID: 10.",
                                      "instance": "/api/v1/segunda-mano/10/retire",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Publicación no encontrada",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "PublicacionNoEncontradaRetiro",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/not-found",
                                      "title": "Recurso no encontrado",
                                      "status": 404,
                                      "detail": "No existe publicación con ID: 999",
                                      "instance": "/api/v1/segunda-mano/999/retire",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "No se puede retirar una prenda ya donada",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "PrendaYaDonadaRetiro",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/conflict",
                                      "title": "Conflicto de dominio",
                                      "status": 409,
                                      "detail": "No se puede retirar una prenda que ya ha sido donada a una fundación.",
                                      "instance": "/api/v1/segunda-mano/10/retire",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "ErrorServidorRetiro",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/internal-server-error",
                                      "title": "Error interno del servidor",
                                      "status": 500,
                                      "detail": "Error inesperado al retirar la publicación de la vitrina.",
                                      "instance": "/api/v1/segunda-mano/10/retire",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            )
    })
    @PatchMapping("/{id}/retire")
    @PreAuthorize("hasAnyRole('USER', 'STORE', 'FOUNDATION', 'ADMIN')")
    public ResponseEntity<Void> retirePublication(
            @Parameter(description = "ID de la publicación a retirar", example = "10")
            @PathVariable Long id,
            @Parameter(description = "ID del usuario propietario", example = "101")
            @RequestParam Long userId) {
        resourceOwnershipValidator.validateOwnerOrAdmin(userId);
        managePublicationUseCase.retire(userId, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Destinar prenda hacia canal de donación",
            description = "Transiciona una prenda disponible de segunda mano al estado IN_DONATION para iniciar su postulación benéfica hacia una fundación aliada. Requiere ser propietario.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Prenda destinada exitosamente al canal benéfico de donaciones",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PublicationResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "PrendaDestinadaDonacion",
                                    value = """
                                    {
                                      "id": 10,
                                      "userId": 101,
                                      "garmentName": "Chaqueta Denim Vintage",
                                      "description": "Chaqueta denim estilo retro para donación",
                                      "garmentType": "UPPER",
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
                    description = "No autenticado",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "NoAutenticado",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/unauthorized",
                                      "title": "No autenticado",
                                      "status": 401,
                                      "detail": "Token Bearer JWT ausente o inválido.",
                                      "instance": "/api/v1/segunda-mano/10/donate",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "No es propietario de la prenda",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "NoEsPropietarioDonacion",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/forbidden",
                                      "title": "Acceso denegado",
                                      "status": 403,
                                      "detail": "Acceso denegado: El usuario autenticado (ID: 102) no es propietario de la prenda ID: 10.",
                                      "instance": "/api/v1/segunda-mano/10/donate",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Publicación no encontrada",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "PublicacionNoEncontradaDonacion",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/not-found",
                                      "title": "Recurso no encontrado",
                                      "status": 404,
                                      "detail": "No existe publicación con ID: 999",
                                      "instance": "/api/v1/segunda-mano/999/donate",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "La publicación no está disponible para donar",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "NoDisponibleParaDonar",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/conflict",
                                      "title": "Conflicto de dominio",
                                      "status": 409,
                                      "detail": "La publicación no se encuentra en estado AVAILABLE para ser transferida al canal benéfico.",
                                      "instance": "/api/v1/segunda-mano/10/donate",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "ErrorServidorDonacion",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/internal-server-error",
                                      "title": "Error interno del servidor",
                                      "status": 500,
                                      "detail": "Error inesperado al destinar la prenda al canal de donación.",
                                      "instance": "/api/v1/segunda-mano/10/donate",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            )
    })
    @PatchMapping("/{id}/donate")
    @PreAuthorize("hasAnyRole('USER', 'STORE', 'FOUNDATION', 'ADMIN')")
    public ResponseEntity<PublicationResponseDTO> markForDonation(
            @Parameter(description = "ID de la publicación a donar", example = "10")
            @PathVariable Long id,
            @Parameter(description = "ID del usuario donante propietario", example = "101")
            @RequestParam Long userId) {
        resourceOwnershipValidator.validateOwnerOrAdmin(userId);
        SecondHandPublication updated = managePublicationUseCase.markForDonation(userId, id);
        return ResponseEntity.ok(publicationRestMapper.toResponse(updated));
    }
}
