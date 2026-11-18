package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import edu.dosw.styleradar.donaciones.application.dto.response.PhotoUploadResponseDTO;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidPublicationException;
import edu.dosw.styleradar.donaciones.domain.port.in.UploadGarmentPhotoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST para la carga de fotografías de prendas de segunda mano y donaciones.
 * Implementa el adaptador de entrada en la arquitectura hexagonal para Azure Blob Storage.
 */
@RestController
@RequestMapping("/api/v1/second-hand/photos")
@Tag(
        name = "Garment Photo Upload (Azure Blob Storage)",
        description = "Endpoints for uploading garment images to Azure Blob Storage and retrieving public CDN URLs for the Circular Economy catalog"
)
public class PhotoUploadController {

    private final UploadGarmentPhotoUseCase uploadGarmentPhotoUseCase;

    public PhotoUploadController(UploadGarmentPhotoUseCase uploadGarmentPhotoUseCase) {
        this.uploadGarmentPhotoUseCase = uploadGarmentPhotoUseCase;
    }

    @Operation(
            summary = "Upload garment photo to Azure Blob Storage",
            description = "Uploads a binary image file (JPEG, PNG, WEBP, max 5 MB) to the 'donaciones-fotos' Azure Blob Storage container. Returns the permanent public URL to attach to a publication via POST /api/v1/second-hand/publications.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Photo uploaded successfully to Azure Blob Storage",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PhotoUploadResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "UploadSuccess",
                                    value = """
                                    {
                                      "photoUrl": "https://srdonacionesjmunar.blob.core.windows.net/donaciones-fotos/garment_9b83b9c0a1b2c3d4.jpg",
                                      "filename": "garment_9b83b9c0a1b2c3d4.jpg",
                                      "sizeBytes": 245800
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid file: empty, exceeds 5 MB, or unsupported format",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "InvalidFile",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/bad-request",
                                      "title": "Solicitud inválida",
                                      "status": 400,
                                      "detail": "Tipo de archivo no permitido: text/plain. Solo se admiten formatos JPEG, PNG y WEBP.",
                                      "instance": "/api/v1/second-hand/photos",
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
                                    name = "Unauthorized",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/unauthorized",
                                      "title": "No autenticado",
                                      "status": 401,
                                      "detail": "Token Bearer JWT ausente, expirado o con firma no válida.",
                                      "instance": "/api/v1/second-hand/photos",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden: insufficient role permissions",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "Forbidden",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/forbidden",
                                      "title": "Acceso denegado",
                                      "status": 403,
                                      "detail": "Acceso denegado: Se requiere rol USER, STORE, FOUNDATION o ADMIN para subir fotos.",
                                      "instance": "/api/v1/second-hand/photos",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error while processing binary upload",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "ServerError",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/internal-server-error",
                                      "title": "Error interno del servidor",
                                      "status": 500,
                                      "detail": "Error de entrada/salida al transferir el flujo binario de la imagen hacia Azure Blob Storage.",
                                      "instance": "/api/v1/second-hand/photos",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            )
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('USER', 'STORE', 'FOUNDATION', 'ADMIN')")
    public ResponseEntity<PhotoUploadResponseDTO> uploadPhoto(
            @Parameter(description = "Binary image file (JPEG, PNG, WEBP, max 5 MB)", required = true)
            @RequestParam("file") MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new InvalidPublicationException("Debe seleccionar un archivo de imagen válido para subir");
        }

        try {
            byte[] bytes = file.getBytes();
            String originalFilename = file.getOriginalFilename();
            String contentType = file.getContentType();

            String photoUrl = uploadGarmentPhotoUseCase.uploadPhoto(bytes, originalFilename, contentType);
            String filename = photoUrl.substring(photoUrl.lastIndexOf("/") + 1);

            PhotoUploadResponseDTO response = new PhotoUploadResponseDTO(photoUrl, filename, bytes.length);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException e) {
            throw new RuntimeException("Error de entrada/salida al leer el archivo de fotografía: " + e.getMessage(), e);
        }
    }
}
