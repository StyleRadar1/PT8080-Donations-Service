package edu.dosw.styleradar.donaciones.domain.model;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Tallas estandarizadas de prendas, pantalones y calzado en el ecosistema StyleRadar.
 */
public enum Size {
    XS("XS"),
    S("S"),
    M("M"),
    L("L"),
    XL("XL"),
    XXL("XXL"),
    TALLA_28("28"),
    TALLA_30("30"),
    TALLA_32("32"),
    TALLA_34("34"),
    TALLA_36("36"),
    TALLA_37("37"),
    TALLA_38("38"),
    TALLA_39("39"),
    TALLA_40("40"),
    TALLA_41("41"),
    TALLA_42("42"),
    UNICA("Unica");

    // Alias estático de compatibilidad retroactiva en código Java
    public static final Size ONE_SIZE = UNICA;

    private static final Map<String, Size> LOOKUP = new HashMap<>();

    static {
        for (Size size : values()) {
            LOOKUP.put(normalize(size.name()), size);
            LOOKUP.put(normalize(size.value), size);
        }
        LOOKUP.put("ONE_SIZE", UNICA);
        LOOKUP.put("TALLA_UNICA", UNICA);
    }

    private final String value;

    Size(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Resuelve una talla aceptando valores literales ("28", "38", "Unica", "XS"),
     * nombres de constante ("TALLA_28", "UNICA") y alias previos ("ONE_SIZE").
     */
    @JsonCreator
    public static Size fromString(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        Size matched = LOOKUP.get(normalize(rawValue));
        if (matched != null) {
            return matched;
        }
        throw new IllegalArgumentException("Talla no válida: '" + rawValue + "'");
    }

    private static String normalize(String raw) {
        String noAccents = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return noAccents.toUpperCase(Locale.ROOT).replace(" ", "_").replace("-", "_");
    }
}
