package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

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
 * Controlador REST para la gestión de publicaciones de prendas de segunda mano.
 */
@RestController
@RequestMapping("/segunda-mano")
@RequiredArgsConstructor
@Tag(name = "Publicaciones de Segunda Mano", description = "Endpoints para la gestión del catálogo de reventa C2C de ropa de segunda mano")
public class SecondHandPublicationController {

    private final CreatePublicationUseCase createPublicationUseCase;
    private final GetPublicationsUseCase getPublicationsUseCase;
    private final ManagePublicationUseCase managePublicationUseCase;
    private final PublicationRestMapper publicationRestMapper;

    @Operation(summary = "Crear publicación de segunda mano", description = "Registra una nueva prenda para reventa en el catálogo con fotos opcionales (máximo 4)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Publicación creada exitosamente",
                    content = @Content(schema = @Schema(implementation = PublicationResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o reglas de negocio infringidas")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PublicationResponseDTO> createPublication(
            @Valid @RequestBody CreatePublicationRequestDTO request) {
        CreatePublicationCommand command = publicationRestMapper.toCommand(request);
        SecondHandPublication created = createPublicationUseCase.create(command);
        PublicationResponseDTO response = publicationRestMapper.toResponse(created);
        return ResponseEntity.created(URI.create("/api/v1/segunda-mano/" + response.id())).body(response);
    }

    @Operation(summary = "Obtener publicación por ID", description = "Recupera el detalle completo de una publicación de segunda mano")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Publicación encontrada",
                    content = @Content(schema = @Schema(implementation = PublicationResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Publicación no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PublicationResponseDTO> getPublicationById(
            @Parameter(description = "ID de la publicación a consultar", example = "1")
            @PathVariable Long id) {
        SecondHandPublication publication = getPublicationsUseCase.getById(id);
        return ResponseEntity.ok(publicationRestMapper.toResponse(publication));
    }

    @Operation(summary = "Obtener publicaciones por usuario", description = "Recupera todas las publicaciones creadas por un usuario específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de publicaciones del usuario",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PublicationResponseDTO.class))))
    })
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<PublicationResponseDTO>> getPublicationsByUserId(
            @Parameter(description = "ID del usuario propietario", example = "101")
            @PathVariable Long userId) {
        List<SecondHandPublication> list = getPublicationsUseCase.getByUserId(userId);
        return ResponseEntity.ok(publicationRestMapper.toResponseList(list));
    }

    @Operation(summary = "Buscar publicaciones", description = "Filtra publicaciones en el catálogo según criterios opcionales (tipo, marca, talla, estado, precios, estilo)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de publicaciones coincidentes con los filtros",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PublicationResponseDTO.class))))
    })
    @GetMapping
    public ResponseEntity<List<PublicationResponseDTO>> searchPublications(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) GarmentType garmentType,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) Size size,
            @RequestParam(required = false) ConservationStatus conservationStatus,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) PublicationStatus status,
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

    @Operation(summary = "Editar publicación de segunda mano", description = "Actualiza los atributos de una publicación existente si continúa disponible")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Publicación actualizada exitosamente",
                    content = @Content(schema = @Schema(implementation = PublicationResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de modificación inválidos"),
            @ApiResponse(responseCode = "404", description = "Publicación no encontrada"),
            @ApiResponse(responseCode = "409", description = "La publicación no puede ser modificada en su estado actual o el usuario no es el propietario")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PublicationResponseDTO> editPublication(
            @Parameter(description = "ID de la publicación a editar", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody EditPublicationRequestDTO request) {
        EditPublicationCommand command = publicationRestMapper.toCommand(id, request);
        SecondHandPublication updated = managePublicationUseCase.edit(command);
        return ResponseEntity.ok(publicationRestMapper.toResponse(updated));
    }

    @Operation(summary = "Marcar publicación como vendida", description = "Cambia el estado de una publicación disponible a VENDIDA")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Publicación marcada como vendida exitosamente",
                    content = @Content(schema = @Schema(implementation = PublicationResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Publicación no encontrada"),
            @ApiResponse(responseCode = "409", description = "La publicación no está disponible o el usuario no es el propietario")
    })
    @PatchMapping("/{id}/sell")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PublicationResponseDTO> markAsSold(
            @Parameter(description = "ID de la publicación vendida", example = "1")
            @PathVariable Long id,
            @Parameter(description = "ID del usuario propietario", example = "101")
            @RequestParam Long userId) {
        SecondHandPublication sold = managePublicationUseCase.markAsSold(userId, id);
        return ResponseEntity.ok(publicationRestMapper.toResponse(sold));
    }

    @Operation(summary = "Retirar publicación del catálogo", description = "Retira una publicación disponible impidiendo su venta posterior")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Publicación retirada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Publicación no encontrada"),
            @ApiResponse(responseCode = "409", description = "La publicación no puede ser retirada")
    })
    @PatchMapping("/{id}/retire")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Void> retirePublication(
            @Parameter(description = "ID de la publicación a retirar", example = "1")
            @PathVariable Long id,
            @Parameter(description = "ID del usuario propietario", example = "101")
            @RequestParam Long userId) {
        managePublicationUseCase.retire(userId, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Destinar publicación a donación", description = "Transiciona el estado de la prenda a EN_DONACION para habilitar solicitudes benéficas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Publicación destinada a donación",
                    content = @Content(schema = @Schema(implementation = PublicationResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Publicación no encontrada"),
            @ApiResponse(responseCode = "409", description = "La publicación no puede ser destinada a donación en su estado actual")
    })
    @PatchMapping("/{id}/donate")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PublicationResponseDTO> donatePublication(
            @Parameter(description = "ID de la publicación a destinar a donación", example = "1")
            @PathVariable Long id,
            @Parameter(description = "ID del usuario propietario", example = "101")
            @RequestParam Long userId) {
        SecondHandPublication inDonation = managePublicationUseCase.markForDonation(userId, id);
        return ResponseEntity.ok(publicationRestMapper.toResponse(inDonation));
    }
}
