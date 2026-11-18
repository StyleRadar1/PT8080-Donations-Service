package edu.dosw.styleradar.donaciones.application.dto.request;

import java.math.BigDecimal;
import java.util.List;

import edu.dosw.styleradar.donaciones.domain.exception.InvalidPublicationException;
import edu.dosw.styleradar.donaciones.domain.model.ConservationStatus;
import edu.dosw.styleradar.donaciones.domain.model.GarmentType;
import edu.dosw.styleradar.donaciones.domain.model.LocalidadBogota;
import edu.dosw.styleradar.donaciones.domain.model.Size;
import edu.dosw.styleradar.donaciones.domain.model.Style;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para la solicitud de creación de una nueva publicación en StyleRadar (Segunda Mano C2C o Donación Benéfica).
 */
@Schema(description = "Request payload to create a new circular economy publication in Bogota D.C. (C2C resale or charitable donation)")
public record CreatePublicationRequestDTO(

        @Schema(description = "ID of the owner user or store creating the publication", example = "101")
        @NotNull(message = "El ID del usuario es obligatorio")
        Long userId,

        @Schema(description = "Garment title or descriptive name", example = "Chaqueta Denim Vintage")
        @NotBlank(message = "El nombre de la prenda es obligatorio")
        @jakarta.validation.constraints.Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
        String garmentName,

        @Schema(description = "Detailed description of garment condition and features", example = "Chaqueta denim estilo retro de los 90s en excelente estado")
        @NotBlank(message = "La descripción de la prenda es obligatoria")
        @jakarta.validation.constraints.Size(max = 1000, message = "La descripción no puede exceder 1000 caracteres")
        String description,

        @Schema(description = "Garment category", example = "Chaquetas")
        @NotNull(message = "El tipo de prenda es obligatorio")
        GarmentType garmentType,

        @Schema(description = "Garment brand", example = "Levi's")
        @NotBlank(message = "La marca es obligatoria")
        @jakarta.validation.constraints.Size(max = 80, message = "La marca no puede exceder 80 caracteres")
        String brand,

        @Schema(description = "Predominant color", example = "Azul")
        @NotBlank(message = "El color es obligatorio")
        @jakarta.validation.constraints.Size(max = 50, message = "El color no puede exceder 50 caracteres")
        String color,

        @Schema(description = "Fashion style classification", example = "URBAN")
        @NotNull(message = "El estilo es obligatorio")
        Style style,

        @Schema(description = "Standardized size", example = "M")
        @NotNull(message = "La talla es obligatoria")
        Size size,

        @Schema(description = "Current garment conservation status", example = "LIKE_NEW")
        @NotNull(message = "El estado de conservación es obligatorio")
        ConservationStatus conservationStatus,

        @Schema(description = "Sale price in COP. Forced to 0 COP if esDonacion=true; strictly > 0 COP if esDonacion=false", example = "75000.00")
        @DecimalMin(value = "0.0", message = "El precio debe ser mayor o igual a 0")
        BigDecimal price,

        @Schema(description = "Circular economy discriminator: true for charitable donation (0 COP); false for C2C second-hand resale", example = "false", defaultValue = "false")
        Boolean esDonacion,

        @Schema(description = "Bogota D.C. district (localidad) where the garment is located", example = "CHAPINERO")
        LocalidadBogota localidad,

        @Schema(description = "Garment photo URLs (maximum 4)", example = "[\"https://cdn.styleradar.com/garments/chaqueta1.jpg\"]")
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
     * Valida que las publicaciones de segunda mano C2C tengan un precio estrictamente mayor a 0 COP.
     */
    @AssertTrue(message = "Para publicaciones de segunda mano (venta C2C), el precio debe ser estrictamente mayor a 0 COP")
    public boolean isPriceValidForC2C() {
        if (Boolean.TRUE.equals(esDonacion)) {
            return true;
        }
        return price != null && price.compareTo(BigDecimal.ZERO) > 0;
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
