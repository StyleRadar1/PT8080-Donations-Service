package edu.dosw.styleradar.donaciones.application.dto.request;

import java.math.BigDecimal;
import java.util.List;

import edu.dosw.styleradar.donaciones.domain.model.ConservationStatus;
import edu.dosw.styleradar.donaciones.domain.model.GarmentType;
import edu.dosw.styleradar.donaciones.domain.model.LocalidadBogota;
import edu.dosw.styleradar.donaciones.domain.model.Size;
import edu.dosw.styleradar.donaciones.domain.model.Style;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para la solicitud de edición de una publicación existente en StyleRadar.
 */
@Schema(description = "Datos para la actualización de una publicación de economía circular en Bogotá D.C.")
public record EditPublicationRequestDTO(

        @Schema(description = "ID del usuario propietario que edita la publicación", example = "101")
        @NotNull(message = "El ID del usuario es obligatorio")
        Long userId,

        @Schema(description = "Nombre o título descriptivo de la prenda", example = "Chaqueta Denim Vintage Renovada")
        @NotBlank(message = "El nombre de la prenda es obligatorio")
        @jakarta.validation.constraints.Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
        String garmentName,

        @Schema(description = "Descripción detallada del estado o características", example = "Chaqueta denim estilo retro de los 90s con botones originales")
        @jakarta.validation.constraints.Size(max = 1000, message = "La descripción no puede exceder 1000 caracteres")
        String description,

        @Schema(description = "Categoría o tipo de prenda textil", example = "UPPER")
        @NotNull(message = "El tipo de prenda es obligatorio")
        GarmentType garmentType,

        @Schema(description = "Marca fabricante de la prenda", example = "Levi's")
        @NotBlank(message = "La marca es obligatoria")
        @jakarta.validation.constraints.Size(max = 80, message = "La marca no puede exceder 80 caracteres")
        String brand,

        @Schema(description = "Color predominante", example = "Azul Claro")
        @NotBlank(message = "El color es obligatorio")
        @jakarta.validation.constraints.Size(max = 50, message = "El color no puede exceder 50 caracteres")
        String color,

        @Schema(description = "Estilo de moda clasificado", example = "URBAN")
        @NotNull(message = "El estilo es obligatorio")
        Style style,

        @Schema(description = "Talla estandarizada", example = "M")
        @NotNull(message = "La talla es obligatoria")
        Size size,

        @Schema(description = "Grado o estado de conservación actual", example = "GOOD")
        @NotNull(message = "El estado de conservación es obligatorio")
        ConservationStatus conservationStatus,

        @Schema(description = "Precio de venta en COP (>= 0). Si es prenda de donación, se mantendrá en 0 COP", example = "68000.00")
        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.0", message = "El precio debe ser mayor o igual a 0")
        BigDecimal price,

        @Schema(description = "Localidad de Bogotá D.C. donde se ubica la prenda", example = "CHAPINERO")
        LocalidadBogota localidad,

        @Schema(description = "URLs de fotografías de la prenda (máximo 4)", example = "[\"https://cdn.styleradar.com/garments/chaqueta_editada.jpg\"]")
        @jakarta.validation.constraints.Size(max = 4, message = "No se permiten más de 4 fotografías")
        List<String> photos
) {
    public EditPublicationRequestDTO {
        if (localidad == null) {
            localidad = LocalidadBogota.CHAPINERO;
        }
    }

    /**
     * Constructor de sobrecarga para compatibilidad hacia atrás.
     */
    public EditPublicationRequestDTO(
            Long userId,
            String garmentName,
            String description,
            GarmentType garmentType,
            String brand,
            String color,
            Style style,
            Size size,
            ConservationStatus conservationStatus,
            BigDecimal price,
            List<String> photos
    ) {
        this(userId, garmentName, description, garmentType, brand, color, style, size, conservationStatus, price, LocalidadBogota.CHAPINERO, photos);
    }
}
