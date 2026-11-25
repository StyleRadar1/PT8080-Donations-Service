package edu.dosw.styleradar.donaciones.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalidadBogotaTest {

    @Test
    @DisplayName("Debe contener exactamente las 20 localidades oficiales de Bogotá D.C.")
    void shouldContainExactlyTwentyLocalities() {
        assertThat(LocalidadBogota.values()).hasSize(20);
        assertThat(LocalidadBogota.values()).contains(
                LocalidadBogota.USAQUEN,
                LocalidadBogota.CHAPINERO,
                LocalidadBogota.SANTA_FE,
                LocalidadBogota.SAN_CRISTOBAL,
                LocalidadBogota.USME,
                LocalidadBogota.TUNJUELITO,
                LocalidadBogota.BOSA,
                LocalidadBogota.KENNEDY,
                LocalidadBogota.FONTIBON,
                LocalidadBogota.ENGATIVA,
                LocalidadBogota.SUBA,
                LocalidadBogota.BARRIOS_UNIDOS,
                LocalidadBogota.TEUSAQUILLO,
                LocalidadBogota.LOS_MARTIRES,
                LocalidadBogota.ANTONIO_NARINO,
                LocalidadBogota.PUENTE_ARANDA,
                LocalidadBogota.LA_CANDELARIA,
                LocalidadBogota.RAFAEL_URIBE_URIBE,
                LocalidadBogota.CIUDAD_BOLIVAR,
                LocalidadBogota.SUMAPAZ
        );
    }

    @Test
    @DisplayName("Debe parsear nombres de localidades de forma tolerante a mayúsculas, minúsculas y espacios")
    void shouldParseLocalitiesTolerantly() {
        assertThat(LocalidadBogota.fromString("chapinero")).isEqualTo(LocalidadBogota.CHAPINERO);
        assertThat(LocalidadBogota.fromString("San Cristobal")).isEqualTo(LocalidadBogota.SAN_CRISTOBAL);
        assertThat(LocalidadBogota.fromString("CIUDAD-BOLIVAR")).isEqualTo(LocalidadBogota.CIUDAD_BOLIVAR);
        assertThat(LocalidadBogota.fromString(null)).isNull();
        assertThat(LocalidadBogota.fromString("   ")).isNull();
    }

    @Test
    @DisplayName("Debe lanzar IllegalArgumentException para localidades ajenas a Bogotá D.C.")
    void shouldThrowForNonBogotaLocality() {
        assertThatThrownBy(() -> LocalidadBogota.fromString("Medellin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("StyleRadar opera exclusivamente en las 20 localidades de Bogotá D.C.");
    }
}
