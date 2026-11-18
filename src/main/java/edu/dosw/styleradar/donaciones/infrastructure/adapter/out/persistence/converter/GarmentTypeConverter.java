package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.converter;

import edu.dosw.styleradar.donaciones.domain.model.GarmentType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Convertidor JPA tolerante para {@link GarmentType}, capaz de persistir valores canónicos
 * y leer registros históricos heredados (UPPER, LOWER, DRESS, FOOTWEAR, ACCESSORY o etiquetas).
 */
@Converter
public class GarmentTypeConverter implements AttributeConverter<GarmentType, String> {

    @Override
    public String convertToDatabaseColumn(GarmentType attribute) {
        return attribute != null ? attribute.name() : null;
    }

    @Override
    public GarmentType convertToEntityAttribute(String dbData) {
        return GarmentType.fromString(dbData);
    }
}
