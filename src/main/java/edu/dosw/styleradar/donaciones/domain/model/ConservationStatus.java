package edu.dosw.styleradar.donaciones.domain.model;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Grados del estado de conservación de una prenda de segunda mano.
 */
public enum ConservationStatus {
    NEW_WITH_TAGS,
    LIKE_NEW,
    GOOD_CONDITION,
    ACCEPTABLE;

    private static final Map<String, ConservationStatus> ALIAS_MAP = new HashMap<>();

    static {
        for (ConservationStatus status : values()) {
            ALIAS_MAP.put(normalize(status.name()), status);
        }
        ALIAS_MAP.put("NUEVO_CON_ETIQUETAS", NEW_WITH_TAGS);
        ALIAS_MAP.put("COMO_NUEVO", LIKE_NEW);
        ALIAS_MAP.put("BUEN_ESTADO", GOOD_CONDITION);
        ALIAS_MAP.put("MUY_BUENO", GOOD_CONDITION);
        ALIAS_MAP.put("MUY_BUEN_ESTADO", GOOD_CONDITION);
        ALIAS_MAP.put("GOOD", GOOD_CONDITION);
        ALIAS_MAP.put("ACEPTABLE", ACCEPTABLE);
        ALIAS_MAP.put("BUENO", ACCEPTABLE);
        ALIAS_MAP.put("CON_DETALLES", ACCEPTABLE);
    }

    /**
     * Resuelve un estado de conservación tolerando minúsculas, espacios, guiones y alias en español.
     */
    @JsonCreator
    public static ConservationStatus fromString(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        ConservationStatus matched = ALIAS_MAP.get(normalize(rawValue));
        if (matched != null) {
            return matched;
        }
        throw new IllegalArgumentException("Estado de conservación no válido: '" + rawValue + "'");
    }

    private static String normalize(String raw) {
        String noAccents = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return noAccents.toUpperCase(Locale.ROOT).replace(" ", "_").replace("-", "_");
    }
}
