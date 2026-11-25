package edu.dosw.styleradar.donaciones.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de respuesta tras una carga exitosa de fotografía en Azure Blob Storage.
 */
@Schema(description = "Response after successful garment photo upload to cloud storage")
public record PhotoUploadResponseDTO(

        @Schema(description = "Public direct access URL for the uploaded photo", example = "https://srdonacionesjmunar.blob.core.windows.net/donaciones-fotos/garment_a1b2c3d4.jpg")
        String photoUrl,

        @Schema(description = "Unique filename assigned inside the storage container", example = "garment_a1b2c3d4.jpg")
        String filename,

        @Schema(description = "Uploaded file size in bytes", example = "204850")
        long sizeBytes
) {
}
