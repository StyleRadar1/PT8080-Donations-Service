package edu.dosw.styleradar.donaciones.domain.model;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Categorías oficiales de prendas aceptadas en el catálogo de Segunda Vida y Donaciones de StyleRadar.
 * Conserva alias estáticos e interoperables para compatibilidad con integraciones previas y con el frontend.
 */
public enum GarmentType {
    CAMISAS("Camisas"),
    CAMISETAS("Camisetas"),
    SUETERES("Suéteres"),
    CHAQUETAS("Chaquetas"),
    PANTALONES("Pantalones"),
    FALDAS("Faldas"),
    VESTIDOS("Vestidos"),
    CALZADO("Calzado"),
    ACCESORIOS("Accesorios");

    // Alias estáticos de compatibilidad retroactiva en código Java
    public static final GarmentType UPPER = CAMISAS;
    public static final GarmentType LOWER = PANTALONES;
    public static final GarmentType DRESS = VESTIDOS;
    public static final GarmentType FOOTWEAR = CALZADO;
    public static final GarmentType ACCESSORY = ACCESORIOS;

    private static final Map<String, GarmentType> ALIAS_MAP = new HashMap<>();

    static {
        for (GarmentType type : values()) {
            ALIAS_MAP.put(normalize(type.name()), type);
            ALIAS_MAP.put(normalize(type.label), type);
        }
        // Variantes singulares y alias de versiones anteriores / otros microservicios
        ALIAS_MAP.put("CAMISA", CAMISAS);
        ALIAS_MAP.put("UPPER", CAMISAS);
        ALIAS_MAP.put("SUPERIOR", CAMISAS);
        ALIAS_MAP.put("CAMISETA", CAMISETAS);
        ALIAS_MAP.put("SUETER", SUETERES);
        ALIAS_MAP.put("BUSO", SUETERES);
        ALIAS_MAP.put("BUSOS", SUETERES);
        ALIAS_MAP.put("BUZO", SUETERES);
        ALIAS_MAP.put("BUZOS", SUETERES);
        ALIAS_MAP.put("SACO", SUETERES);
        ALIAS_MAP.put("SACOS", SUETERES);
        ALIAS_MAP.put("SWEATER", SUETERES);
        ALIAS_MAP.put("CHAQUETA", CHAQUETAS);
        ALIAS_MAP.put("ABRIGO", CHAQUETAS);
        ALIAS_MAP.put("ABRIGOS", CHAQUETAS);
        ALIAS_MAP.put("PANTALON", PANTALONES);
        ALIAS_MAP.put("JEANS", PANTALONES);
        ALIAS_MAP.put("LOWER", PANTALONES);
        ALIAS_MAP.put("INFERIOR", PANTALONES);
        ALIAS_MAP.put("FALDA", FALDAS);
        ALIAS_MAP.put("VESTIDO", VESTIDOS);
        ALIAS_MAP.put("CONJUNTO", VESTIDOS);
        ALIAS_MAP.put("CONJUNTOS", VESTIDOS);
        ALIAS_MAP.put("DRESS", VESTIDOS);
        ALIAS_MAP.put("FULL_BODY", VESTIDOS);
        ALIAS_MAP.put("CUERPO_COMPLETO", VESTIDOS);
        ALIAS_MAP.put("ZAPATOS", CALZADO);
        ALIAS_MAP.put("TENIS", CALZADO);
        ALIAS_MAP.put("FOOTWEAR", CALZADO);
        ALIAS_MAP.put("ACCESORIO", ACCESORIOS);
        ALIAS_MAP.put("ACCESSORY", ACCESORIOS);
        ALIAS_MAP.put("ACCESSORIES", ACCESORIOS);
    }

    private final String label;

    GarmentType(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    /**
     * Resuelve una categoría de forma tolerante a mayúsculas, minúsculas, tildes y alias previos.
     */
    @JsonCreator
    public static GarmentType fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        GarmentType matched = ALIAS_MAP.get(normalize(value));
        if (matched != null) {
            return matched;
        }
        throw new IllegalArgumentException("Categoría de prenda no válida: '" + value + "'");
    }

    private static String normalize(String raw) {
        String noAccents = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return noAccents.toUpperCase(Locale.ROOT).replace(" ", "_").replace("-", "_");
    }
}
