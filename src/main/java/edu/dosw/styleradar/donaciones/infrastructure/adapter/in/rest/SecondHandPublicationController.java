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
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controlador REST para la gestión de publicaciones de prendas de segunda mano.
 */
@RestController
@RequestMapping("/segunda-mano")
@RequiredArgsConstructor
@Tag(name = "Publicaciones de Segunda Mano", description = "Endpoints para la gestión del catálogo de reventa C2C de ropa de segunda mano (SR-RF-33 a SR-RF-39 y SR-RF-42)")
public class SecondHandPublicationController {

    private final CreatePublicationUseCase createPublicationUseCase;
    private final GetPublicationsUseCase getPublicationsUseCase;
    private final ManagePublicationUseCase managePublicationUseCase;
    private final PublicationRestMapper publicationRestMapper;
    private final ResourceOwnershipValidator resourceOwnershipValidator;

    @Operation(
            summary = "Crear publicación de segunda mano (SR-RF-33, SR-RF-34, SR-RF-35)",
            description = "Registra una nueva prenda para reventa en el catálogo C2C con hasta 4 fotografías, clasificación de estado de conservación y precio en COP. Requiere rol USER o ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Publicación creada exitosamente",
                    content = @Content(schema = @Schema(implementation = PublicationResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o faltantes",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado o token JWT inválido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: rol insuficiente",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PublicationResponseDTO> createPublication(
            @Valid @RequestBody CreatePublicationRequestDTO request) {
        resourceOwnershipValidator.validateOwnerOrAdmin(request.userId());
        CreatePublicationCommand command = publicationRestMapper.toCommand(request);
        SecondHandPublication created = createPublicationUseCase.create(command);
        PublicationResponseDTO response = publicationRestMapper.toResponse(created);
        return ResponseEntity.created(URI.create("/api/v1/segunda-mano/" + response.id())).body(response);
    }

    @Operation(
            summary = "Obtener publicación por ID (SR-RF-36)",
            description = "Recupera el detalle completo de una publicación de prenda usada en el catálogo. Endpoint público de lectura."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Publicación encontrada",
                    content = @Content(schema = @Schema(implementation = PublicationResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Publicación no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<PublicationResponseDTO> getPublicationById(
            @Parameter(description = "ID de la publicación a consultar", example = "1")
            @PathVariable Long id) {
        SecondHandPublication publication = getPublicationsUseCase.getById(id);
        return ResponseEntity.ok(publicationRestMapper.toResponse(publication));
    }

    @Operation(
            summary = "Obtener publicaciones por usuario (SR-RF-36)",
            description = "Recupera todas las publicaciones creadas por un usuario específico (armario personal / prendas en venta). Requiere rol USER o ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de publicaciones del usuario",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PublicationResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado o token JWT inválido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: rol insuficiente",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<PublicationResponseDTO>> getPublicationsByUserId(
            @Parameter(description = "ID del usuario propietario", example = "101")
            @PathVariable Long userId) {
        resourceOwnershipValidator.validateOwnerOrAdmin(userId);
        List<SecondHandPublication> list = getPublicationsUseCase.getByUserId(userId);
        return ResponseEntity.ok(publicationRestMapper.toResponseList(list));
    }

    @Operation(
            summary = "Buscar publicaciones en catálogo (SR-RF-36)",
            description = "Motor de búsqueda con filtros combinados por término de texto, categoría de prenda, marca, color, talla, estado de conservación, rango de precios, estado y estilo. Endpoint público."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de publicaciones coincidentes con los filtros",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PublicationResponseDTO.class)))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public ResponseEntity<List<PublicationResponseDTO>> searchPublications(
            @Parameter(description = "Término textual para buscar en nombre o descripción", example = "Chaqueta")
            @RequestParam(required = false) String searchTerm,

            @Parameter(description = "Tipo o categoría de prenda (UPPER, LOWER, DRESS, FOOTWEAR, ACCESSORY)", example = "UPPER")
            @RequestParam(required = false) GarmentType garmentType,

            @Parameter(description = "Marca fabricante de la prenda", example = "Levi's")
            @RequestParam(required = false) String brand,

            @Parameter(description = "Color predominante de la prenda", example = "Azul")
            @RequestParam(required = false) String color,

            @Parameter(description = "Talla estandarizada (XS, S, M, L, XL, XXL, ONE_SIZE)", example = "M")
            @RequestParam(required = false) Size size,

            @Parameter(description = "Estado de conservación (BRAND_NEW_WITH_TAGS, LIKE_NEW, GOOD_CONDITION, ACCEPTABLE)", example = "LIKE_NEW")
            @RequestParam(required = false) ConservationStatus conservationStatus,

            @Parameter(description = "Precio mínimo en COP para filtrar", example = "20000.00")
            @RequestParam(required = false) BigDecimal minPrice,

            @Parameter(description = "Precio máximo en COP para filtrar", example = "100000.00")
            @RequestParam(required = false) BigDecimal maxPrice,

            @Parameter(description = "Estado de la publicación (AVAILABLE, SOLD, DONATED, IN_DONATION, RETIRED)", example = "AVAILABLE")
            @RequestParam(required = false) PublicationStatus status,

            @Parameter(description = "Estilo de moda (CASUAL, FORMAL, SPORT, URBAN, VINTAGE)", example = "URBAN")
            @RequestParam(required = false) Style style) {

        PublicationSearchCriteria criteria = PublicationSearchCriteria.builder()
                .searchTerm(searchTerm)
                .type(garmentType)
                .brand(brand)
                .color(color)
                .size(size)
                .conservationStatus(conservationStatus)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .status(status)
                .style(style)
                .build();

        List<SecondHandPublication> results = getPublicationsUseCase.search(criteria);
        return ResponseEntity.ok(publicationRestMapper.toResponseList(results));
    }

    @Operation(
            summary = "Editar publicación de segunda mano (SR-RF-37)",
            description = "Actualiza los atributos de una prenda publicada si continúa en estado AVAILABLE y el usuario solicitante es el propietario. Requiere rol USER o ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Publicación actualizada exitosamente",
                    content = @Content(schema = @Schema(implementation = PublicationResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de modificación inválidos",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado o token JWT inválido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: rol insuficiente",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Publicación no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "La publicación no puede ser modificada en su estado actual o el usuario no es el propietario",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PublicationResponseDTO> editPublication(
            @Parameter(description = "ID de la publicación a editar", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody EditPublicationRequestDTO request) {
        resourceOwnershipValidator.validateOwnerOrAdmin(request.userId());
        EditPublicationCommand command = publicationRestMapper.toCommand(id, request);
        SecondHandPublication updated = managePublicationUseCase.edit(command);
        return ResponseEntity.ok(publicationRestMapper.toResponse(updated));
    }

    @Operation(
            summary = "Marcar publicación como vendida (SR-RF-38)",
            description = "Transiciona el estado de la prenda a VENDIDA (SOLD), retirándola del catálogo de compraventa y notificando a ms-chat con el compradorId para preservar la conversación del comprador y cerrar las demás. Requiere rol USER o ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Publicación marcada como vendida exitosamente",
                    content = @Content(schema = @Schema(implementation = PublicationResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Parámetro obligatorio faltante (compradorId)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado o token JWT inválido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: rol insuficiente",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Publicación no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "La publicación no está disponible o el usuario no es el propietario",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{id}/sell")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PublicationResponseDTO> markAsSold(
            @Parameter(description = "ID de la publicación vendida", example = "1")
            @PathVariable Long id,
            @Parameter(description = "ID del usuario propietario", example = "101")
            @RequestParam Long userId,
            @Parameter(description = "ID del comprador en el chat C2C (obligatorio para preservar la conversación en ms-chat)", example = "102", required = true)
            @RequestParam Long compradorId) {
        resourceOwnershipValidator.validateOwnerOrAdmin(userId);
        SecondHandPublication sold = managePublicationUseCase.markAsSold(userId, id, compradorId);
        return ResponseEntity.ok(publicationRestMapper.toResponse(sold));
    }

    @Operation(
            summary = "Retirar publicación del catálogo (SR-RF-39)",
            description = "Retira voluntariamente una publicación disponible impidiendo su venta posterior. Requiere rol USER o ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Publicación retirada exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado o token JWT inválido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: rol insuficiente",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Publicación no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "La publicación no puede ser retirada en su estado actual",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{id}/retire")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Void> retirePublication(
            @Parameter(description = "ID de la publicación a retirar", example = "1")
            @PathVariable Long id,
            @Parameter(description = "ID del usuario propietario", example = "101")
            @RequestParam Long userId) {
        resourceOwnershipValidator.validateOwnerOrAdmin(userId);
        managePublicationUseCase.retire(userId, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Destinar publicación a donación (SR-RF-42)",
            description = "Transiciona el estado de la prenda a EN_DONACION (IN_DONATION) para habilitar solicitudes benéficas hacia fundaciones. Requiere rol USER o ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Publicación destinada a donación exitosamente",
                    content = @Content(schema = @Schema(implementation = PublicationResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado o token JWT inválido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: rol insuficiente",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Publicación no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "La publicación no puede ser destinada a donación en su estado actual",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{id}/donate")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PublicationResponseDTO> donatePublication(
            @Parameter(description = "ID de la publicación a destinar a donación", example = "1")
            @PathVariable Long id,
            @Parameter(description = "ID del usuario propietario", example = "101")
            @RequestParam Long userId) {
        resourceOwnershipValidator.validateOwnerOrAdmin(userId);
        SecondHandPublication inDonation = managePublicationUseCase.markForDonation(userId, id);
        return ResponseEntity.ok(publicationRestMapper.toResponse(inDonation));
    }
}
