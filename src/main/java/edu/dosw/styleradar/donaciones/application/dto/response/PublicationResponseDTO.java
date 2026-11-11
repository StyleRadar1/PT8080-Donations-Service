package edu.dosw.styleradar.donaciones.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import edu.dosw.styleradar.donaciones.domain.model.ConservationStatus;
import edu.dosw.styleradar.donaciones.domain.model.GarmentType;
import edu.dosw.styleradar.donaciones.domain.model.PublicationStatus;
import edu.dosw.styleradar.donaciones.domain.model.Size;
import edu.dosw.styleradar.donaciones.domain.model.Style;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de respuesta que representa los datos de una publicación de segunda mano.
 */
@Schema(description = "Respuesta detallada con información de una publicación de segunda mano")
public record PublicationResponseDTO(

        @Schema(description = "Identificador único de la publicación", example = "1")
        Long id,

        @Schema(description = "ID del usuario propietario", example = "101")
        Long userId,

        @Schema(description = "Nombre o título de la prenda", example = "Chaqueta Denim Vintage")
        String garmentName,

        @Schema(description = "Descripción detallada", example = "Chaqueta denim estilo retro de los 90s")
        String description,

        @Schema(description = "Tipo o categoría de prenda textil", example = "UPPER")
        GarmentType garmentType,

        @Schema(description = "Marca de la prenda", example = "Levi's")
        String brand,

        @Schema(description = "Color de la prenda", example = "Azul")
        String color,

        @Schema(description = "Estilo de la prenda", example = "URBAN")
        Style style,

        @Schema(description = "Talla estandarizada", example = "M")
        Size size,

        @Schema(description = "Estado de conservación de la prenda", example = "LIKE_NEW")
        ConservationStatus conservationStatus,

        @Schema(description = "Precio asignado para reventa", example = "75000.00")
        BigDecimal price,

        @Schema(description = "Estado actual de la publicación en el ciclo de vida", example = "AVAILABLE")
        PublicationStatus status,

        @Schema(description = "Fecha y hora de publicación", example = "2026-11-01T10:00:00")
        LocalDateTime publicationDate,

        @Schema(description = "Fecha y hora de última actualización", example = "2026-11-05T14:30:00")
        LocalDateTime updateDate,

        @Schema(description = "Lista de URLs de fotografías", example = "[\"https://cdn.style.com/foto1.jpg\"]")
        List<String> photos
) {
}
