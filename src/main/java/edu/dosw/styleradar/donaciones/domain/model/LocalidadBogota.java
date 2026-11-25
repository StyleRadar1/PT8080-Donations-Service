package edu.dosw.styleradar.donaciones.domain.model;

/**
 * Enumeración oficial de las 20 localidades del Distrito Capital de Bogotá.
 * StyleRadar opera exclusivamente en la ciudad de Bogotá D.C.
 */
public enum LocalidadBogota {
    USAQUEN,
    CHAPINERO,
    SANTA_FE,
    SAN_CRISTOBAL,
    USME,
    TUNJUELITO,
    BOSA,
    KENNEDY,
    FONTIBON,
    ENGATIVA,
    SUBA,
    BARRIOS_UNIDOS,
    TEUSAQUILLO,
    LOS_MARTIRES,
    ANTONIO_NARINO,
    PUENTE_ARANDA,
    LA_CANDELARIA,
    RAFAEL_URIBE_URIBE,
    CIUDAD_BOLIVAR,
    SUMAPAZ;

    /**
     * Parsea un texto de localidad de forma tolerante a mayúsculas/minúsculas y guiones.
     */
    public static LocalidadBogota fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String normalized = text.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        for (LocalidadBogota loc : values()) {
            if (loc.name().equalsIgnoreCase(normalized)) {
                return loc;
            }
        }
        throw new IllegalArgumentException(
                "Localidad no válida: '" + text + "'. StyleRadar opera exclusivamente en las 20 localidades de Bogotá D.C.");
    }
}
