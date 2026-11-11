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
@RequestMapping("/segunda-mano/fotos")
@Tag(
        name = "Carga de Fotografías (Azure Blob Storage)",
        description = "Endpoints para la subida de imágenes de prendas hacia Azure Blob Storage y obtención de URLs públicas CDN para el catálogo de Economía Circular"
)
public class PhotoUploadController {

    private final UploadGarmentPhotoUseCase uploadGarmentPhotoUseCase;

    public PhotoUploadController(UploadGarmentPhotoUseCase uploadGarmentPhotoUseCase) {
        this.uploadGarmentPhotoUseCase = uploadGarmentPhotoUseCase;
    }

    @Operation(
            summary = "Subir fotografía de prenda a Azure Blob Storage",
            description = "Carga un archivo binario de imagen (formatos JPEG, PNG, WEBP, tamaño máximo 5 MB) al contenedor 'donaciones-fotos' de Azure Blob Storage en el Resource Group 'rg-styleradar'. Retorna la URL pública permanente para asociarla a la prenda en el endpoint POST /api/v1/segunda-mano.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Fotografía cargada exitosamente en Azure Blob Storage",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PhotoUploadResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "CargaExitosa",
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
                    description = "Archivo inválido: vacío, excede 5 MB o formato no permitido",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "ArchivoInvalido",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/bad-request",
                                      "title": "Solicitud inválida",
                                      "status": 400,
                                      "detail": "Tipo de archivo no permitido: text/plain. Solo se admiten formatos JPEG, PNG y WEBP.",
                                      "instance": "/api/v1/segunda-mano/fotos/upload",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token JWT ausente/inválido",
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
                                      "detail": "Token Bearer JWT ausente, expirado o con firma no válida.",
                                      "instance": "/api/v1/segunda-mano/fotos/upload",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado: rol insuficiente",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "AccesoDenegado",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/forbidden",
                                      "title": "Acceso denegado",
                                      "status": 403,
                                      "detail": "Acceso denegado: Se requiere rol USER, STORE, FOUNDATION o ADMIN para subir fotos.",
                                      "instance": "/api/v1/segunda-mano/fotos/upload",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno al procesar el flujo binario",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "ErrorServidor",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/internal-server-error",
                                      "title": "Error interno del servidor",
                                      "status": 500,
                                      "detail": "Error de entrada/salida al transferir el flujo binario de la imagen hacia Azure Blob Storage.",
                                      "instance": "/api/v1/segunda-mano/fotos/upload",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            )
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('USER', 'STORE', 'FOUNDATION', 'ADMIN')")
    public ResponseEntity<PhotoUploadResponseDTO> uploadPhoto(
            @Parameter(description = "Archivo binario de imagen (JPEG, PNG, WEBP, máx 5MB)", required = true)
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
