package edu.dosw.styleradar.donaciones.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.dosw.styleradar.donaciones.domain.exception.PublicacionInvalidaException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Entidad Prenda")
class PrendaTest {

    @Test
    @DisplayName("Debe validar exitosamente cuando la prenda tiene datos completos")
    void debeValidarPrendaCorrecta() {
        Prenda prenda = Prenda.builder()
                .id(1L)
                .nombre("Chaqueta Denim Vintage")
                .descripcion("Chaqueta de jean clásico")
                .tipo(TipoPrenda.SUPERIOR)
                .marca("Levi's")
                .color("Azul")
                .estilo(Estilo.URBANO)
                .build();

        assertDoesNotThrow(prenda::validarInvariantes);
        assertEquals("Chaqueta Denim Vintage", prenda.getNombre());
        assertEquals(TipoPrenda.SUPERIOR, prenda.getTipo());
        assertEquals("Levi's", prenda.getMarca());
        assertEquals("Azul", prenda.getColor());
        assertEquals(Estilo.URBANO, prenda.getEstilo());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el nombre de la prenda es nulo o vacío")
    void debeFallarSiNombreEsInvalido() {
        Prenda prendaSinNombre = Prenda.builder()
                .nombre("   ")
                .tipo(TipoPrenda.INFERIOR)
                .build();

        assertThrows(PublicacionInvalidaException.class, prendaSinNombre::validarInvariantes);

        prendaSinNombre.setNombre(null);
        assertThrows(PublicacionInvalidaException.class, prendaSinNombre::validarInvariantes);
    }

    @Test
    @DisplayName("Debe lanzar excepción si el tipo de prenda es nulo")
    void debeFallarSiTipoEsNulo() {
        Prenda prendaSinTipo = Prenda.builder()
                .nombre("Pantalón Cargo")
                .tipo(null)
                .build();

        assertThrows(PublicacionInvalidaException.class, prendaSinTipo::validarInvariantes);
    }
}
