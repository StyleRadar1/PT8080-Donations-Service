package edu.dosw.styleradar.donaciones.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de respuesta tras una carga exitosa de fotografía en Azure Blob Storage.
 */
@Schema(description = "Respuesta de carga exitosa de fotografía en almacenamiento en la nube")
public record PhotoUploadResponseDTO(

        @Schema(description = "URL pública de acceso directo a la fotografía almacenada", example = "https://srdonacionesjmunar.blob.core.windows.net/donaciones-fotos/garment_a1b2c3d4.jpg")
        String photoUrl,

        @Schema(description = "Nombre único asignado al archivo en el contenedor", example = "garment_a1b2c3d4.jpg")
        String filename,

        @Schema(description = "Tamaño del archivo cargado en bytes", example = "204850")
        long sizeBytes
) {
}
