package edu.dosw.styleradar.donaciones.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.dosw.styleradar.donaciones.domain.exception.InvalidPublicationException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para la entidad de dominio Garment.
 */
@DisplayName("Pruebas Unitarias - Entidad Garment")
class GarmentTest {

    @Test
    @DisplayName("Debe validar exitosamente cuando la prenda tiene datos completos")
    void shouldValidateCorrectGarment() {
        Garment garment = Garment.builder()
                .id(1L)
                .name("Chaqueta Denim Vintage")
                .description("Chaqueta de jean clásico")
                .type(GarmentType.UPPER)
                .brand("Levi's")
                .color("Azul")
                .style(Style.URBAN)
                .build();

        assertDoesNotThrow(garment::validateInvariants);
        assertEquals("Chaqueta Denim Vintage", garment.getName());
        assertEquals(GarmentType.UPPER, garment.getType());
        assertEquals("Levi's", garment.getBrand());
        assertEquals("Azul", garment.getColor());
        assertEquals(Style.URBAN, garment.getStyle());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el nombre de la prenda es nulo o vacío")
    void shouldFailWhenNameIsInvalid() {
        Garment garmentWithoutName = Garment.builder()
                .name("   ")
                .type(GarmentType.LOWER)
                .build();

        assertThrows(InvalidPublicationException.class, garmentWithoutName::validateInvariants);

        garmentWithoutName.setName(null);
        assertThrows(InvalidPublicationException.class, garmentWithoutName::validateInvariants);
    }

    @Test
    @DisplayName("Debe lanzar excepción si el tipo de prenda es nulo")
    void shouldFailWhenTypeIsNull() {
        Garment garmentWithoutType = Garment.builder()
                .name("Pantalón Cargo")
                .type(null)
                .build();

        assertThrows(InvalidPublicationException.class, garmentWithoutType::validateInvariants);
    }
}
