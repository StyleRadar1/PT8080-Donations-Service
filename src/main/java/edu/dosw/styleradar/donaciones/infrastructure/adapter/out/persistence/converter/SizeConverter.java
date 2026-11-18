package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.converter;

import edu.dosw.styleradar.donaciones.domain.model.Size;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Convertidor JPA tolerante para {@link Size}, capaz de persistir valores canónicos
 * y leer registros históricos heredados (ONE_SIZE, Unica o tallas numéricas literales como "28").
 */
@Converter
public class SizeConverter implements AttributeConverter<Size, String> {

    @Override
    public String convertToDatabaseColumn(Size attribute) {
        return attribute != null ? attribute.name() : null;
    }

    @Override
    public Size convertToEntityAttribute(String dbData) {
        return Size.fromString(dbData);
    }
}
