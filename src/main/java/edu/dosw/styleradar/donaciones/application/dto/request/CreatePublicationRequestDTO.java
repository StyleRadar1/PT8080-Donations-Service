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
 * DTO para la solicitud de creación de una nueva publicación en StyleRadar (Segunda Mano C2C o Donación Benéfica).
 */
@Schema(description = "Datos para crear una nueva publicación de economía circular en Bogotá D.C.")
public record CreatePublicationRequestDTO(

        @Schema(description = "ID del usuario o entidad propietaria que publica la prenda", example = "101")
        @NotNull(message = "El ID del usuario es obligatorio")
        Long userId,

        @Schema(description = "Nombre o título descriptivo de la prenda", example = "Chaqueta Denim Vintage")
        @NotBlank(message = "El nombre de la prenda es obligatorio")
        @jakarta.validation.constraints.Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
        String garmentName,

        @Schema(description = "Descripción detallada del estado o características", example = "Chaqueta denim estilo retro de los 90s en excelente estado")
        @jakarta.validation.constraints.Size(max = 1000, message = "La descripción no puede exceder 1000 caracteres")
        String description,

        @Schema(description = "Categoría o tipo de prenda textil", example = "UPPER")
        @NotNull(message = "El tipo de prenda es obligatorio")
        GarmentType garmentType,

        @Schema(description = "Marca fabricante de la prenda", example = "Levi's")
        @NotBlank(message = "La marca es obligatoria")
        @jakarta.validation.constraints.Size(max = 80, message = "La marca no puede exceder 80 caracteres")
        String brand,

        @Schema(description = "Color predominante", example = "Azul")
        @NotBlank(message = "El color es obligatorio")
        @jakarta.validation.constraints.Size(max = 50, message = "El color no puede exceder 50 caracteres")
        String color,

        @Schema(description = "Estilo de moda clasificado", example = "URBAN")
        @NotNull(message = "El estilo es obligatorio")
        Style style,

        @Schema(description = "Talla estandarizada", example = "M")
        @NotNull(message = "La talla es obligatoria")
        Size size,

        @Schema(description = "Grado o estado de conservación actual", example = "LIKE_NEW")
        @NotNull(message = "El estado de conservación es obligatorio")
        ConservationStatus conservationStatus,

        @Schema(description = "Precio de venta en COP. Forzoso 0 COP si esDonacion=true; mayor a 0 COP si esDonacion=false", example = "75000.00")
        @DecimalMin(value = "0.0", message = "El precio debe ser mayor o igual a 0")
        BigDecimal price,

        @Schema(description = "Discriminador de economía circular: TRUE para donación benéfica (precio 0 COP, vitrina abierta a fundaciones); FALSE para reventa segunda mano C2C", example = "false", defaultValue = "false")
        Boolean esDonacion,

        @Schema(description = "Localidad de Bogotá D.C. donde se encuentra la prenda (exclusivo para Bogotá)", example = "CHAPINERO")
        LocalidadBogota localidad,

        @Schema(description = "URLs de fotografías de la prenda (máximo 4)", example = "[\"https://cdn.styleradar.com/garments/chaqueta1.jpg\"]")
        @jakarta.validation.constraints.Size(max = 4, message = "No se permiten más de 4 fotografías")
        List<String> photos
) {
    public CreatePublicationRequestDTO {
        if (esDonacion == null) {
            esDonacion = false;
        }
        if (Boolean.TRUE.equals(esDonacion)) {
            price = BigDecimal.ZERO;
        }
        if (localidad == null) {
            localidad = LocalidadBogota.CHAPINERO;
        }
    }

    /**
     * Constructor de sobrecarga para compatibilidad hacia atrás.
     */
    public CreatePublicationRequestDTO(
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
        this(userId, garmentName, description, garmentType, brand, color, style, size, conservationStatus, price, false, LocalidadBogota.CHAPINERO, photos);
    }
}
