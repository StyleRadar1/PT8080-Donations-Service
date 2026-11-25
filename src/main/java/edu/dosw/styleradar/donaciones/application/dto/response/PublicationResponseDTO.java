package edu.dosw.styleradar.donaciones.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import edu.dosw.styleradar.donaciones.domain.model.ConservationStatus;
import edu.dosw.styleradar.donaciones.domain.model.GarmentType;
import edu.dosw.styleradar.donaciones.domain.model.LocalidadBogota;
import edu.dosw.styleradar.donaciones.domain.model.PublicationStatus;
import edu.dosw.styleradar.donaciones.domain.model.Size;
import edu.dosw.styleradar.donaciones.domain.model.Style;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de respuesta que representa los datos de una publicación de economía circular (Segunda Mano C2C o Donación).
 */
@Schema(description = "Detailed response representing a circular economy publication in Bogota D.C.")
public record PublicationResponseDTO(

        @Schema(description = "Unique publication identifier", example = "1")
        Long id,

        @Schema(description = "ID of the owner user or store", example = "101")
        Long userId,

        @Schema(description = "Garment title or name", example = "Chaqueta Denim Vintage")
        String garmentName,

        @Schema(description = "Detailed garment description", example = "Chaqueta denim estilo retro de los 90s")
        String description,

        @Schema(description = "Garment category", example = "Chaquetas")
        GarmentType garmentType,

        @Schema(description = "Garment brand", example = "Levi's")
        String brand,

        @Schema(description = "Garment color", example = "Azul")
        String color,

        @Schema(description = "Fashion style", example = "URBAN")
        Style style,

        @Schema(description = "Standardized size", example = "M")
        Size size,

        @Schema(description = "Garment conservation status", example = "LIKE_NEW")
        ConservationStatus conservationStatus,

        @Schema(description = "Assigned price (0 COP for donation, > 0 COP for second-hand resale)", example = "75000.00")
        BigDecimal price,

        @Schema(description = "Indicates whether the publication is a charitable donation (true) or C2C second-hand resale (false)", example = "false")
        Boolean esDonacion,

        @Schema(description = "Bogota D.C. district (localidad) where the garment is located", example = "CHAPINERO")
        LocalidadBogota localidad,

        @Schema(description = "Current lifecycle status of the publication", example = "AVAILABLE")
        PublicationStatus status,

        @Schema(description = "Version number for optimistic concurrency control", example = "0")
        Long version,

        @Schema(description = "Publication creation timestamp", example = "2026-11-01T10:00:00")
        LocalDateTime publicationDate,

        @Schema(description = "Last update timestamp", example = "2026-11-05T14:30:00")
        LocalDateTime updateDate,

        @Schema(description = "List of photo URLs (maximum 4)", example = "[\"https://cdn.styleradar.com/garments/chaqueta1.jpg\"]")
        List<String> photos
) {
}
