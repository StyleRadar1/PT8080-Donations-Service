package edu.dosw.styleradar.donaciones.domain.model;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.dosw.styleradar.donaciones.domain.exception.InvalidPublicationException;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidStateTransitionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para el agregado raíz SecondHandPublication.
 */
@DisplayName("Pruebas Unitarias - Agregado SecondHandPublication")
class SecondHandPublicationTest {

    private Garment validGarment;

    @BeforeEach
    void setUp() {
        validGarment = Garment.builder()
                .id(10L)
                .name("Camiseta Oversize")
                .type(GarmentType.UPPER)
                .brand("Zara")
                .color("Negro")
                .style(Style.URBAN)
                .build();
    }

    @Test
    @DisplayName("Debe publicar exitosamente con invariantes válidas")
    void shouldPublishSuccessfully() {
        SecondHandPublication pub = SecondHandPublication.builder()
                .userId(1L)
                .garment(validGarment)
                .size(Size.M)
                .conservationStatus(ConservationStatus.LIKE_NEW)
                .price(BigDecimal.valueOf(45000.0))
                .photos(List.of("https://cdn.style.com/foto1.jpg", "https://cdn.style.com/foto2.jpg"))
                .build();

        pub.publish();

        assertEquals(PublicationStatus.AVAILABLE, pub.getStatus());
        assertNotNull(pub.getPublishedAt());
        assertNotNull(pub.getUpdatedAt());
        assertEquals(2, pub.getPhotos().size());
    }

    @Test
    @DisplayName("Debe rechazar publicación con más de 4 fotografías")
    void shouldFailWhenPhotosExceedMax() {
        SecondHandPublication pub = SecondHandPublication.builder()
                .userId(1L)
                .garment(validGarment)
                .size(Size.S)
                .conservationStatus(ConservationStatus.GOOD_CONDITION)
                .price(BigDecimal.valueOf(30000.0))
                .photos(List.of("f1.jpg", "f2.jpg", "f3.jpg", "f4.jpg", "f5.jpg"))
                .build();

        assertThrows(InvalidPublicationException.class, pub::publish);
    }

    @Test
    @DisplayName("Debe rechazar publicación con precio negativo, cero o nulo en segunda mano C2C")
    void shouldFailWhenPriceIsNegativeZeroOrNullInSecondHand() {
        SecondHandPublication pub = SecondHandPublication.builder()
                .userId(1L)
                .garment(validGarment)
                .size(Size.L)
                .conservationStatus(ConservationStatus.ACCEPTABLE)
                .price(BigDecimal.valueOf(-100.0))
                .esDonacion(false)
                .build();

        assertThrows(InvalidPublicationException.class, pub::publish);

        pub.setPrice(BigDecimal.ZERO);
        assertThrows(InvalidPublicationException.class, pub::publish);

        pub.setPrice(null);
        assertThrows(InvalidPublicationException.class, pub::publish);
    }

    @Test
    @DisplayName("Debe rechazar publicación si userId, prenda, talla o conservacion son nulos")
    void shouldFailWhenMandatoryFieldsAreMissing() {
        SecondHandPublication pub = SecondHandPublication.builder().build();
        assertThrows(InvalidPublicationException.class, pub::publish);

        pub.setUserId(1L);
        assertThrows(InvalidPublicationException.class, pub::publish);

        pub.setGarment(validGarment);
        assertThrows(InvalidPublicationException.class, pub::publish);

        pub.setSize(Size.M);
        assertThrows(InvalidPublicationException.class, pub::publish);

        pub.setConservationStatus(ConservationStatus.GOOD_CONDITION);
        assertThrows(InvalidPublicationException.class, pub::publish);
    }

    @Test
    @DisplayName("Debe editar publicación disponible correctamente")
    void shouldEditAvailablePublication() {
        SecondHandPublication pub = SecondHandPublication.builder()
                .userId(1L)
                .garment(validGarment)
                .size(Size.M)
                .conservationStatus(ConservationStatus.LIKE_NEW)
                .price(BigDecimal.valueOf(50000.0))
                .build();
        pub.publish();

        Garment updatedGarment = Garment.builder()
                .id(10L)
                .name("Camiseta Slim")
                .type(GarmentType.UPPER)
                .build();

        pub.edit(updatedGarment, Size.S, ConservationStatus.GOOD_CONDITION,
                BigDecimal.valueOf(40000.0), List.of("foto_nueva.jpg"));

        assertEquals("Camiseta Slim", pub.getGarment().getName());
        assertEquals(Size.S, pub.getSize());
        assertEquals(BigDecimal.valueOf(40000.0), pub.getPrice());
        assertEquals(1, pub.getPhotos().size());
    }

    @Test
    @DisplayName("Debe impedir editar publicación si está vendida, retirada o donada")
    void shouldBlockEditInTerminalStates() {
        SecondHandPublication pub = SecondHandPublication.builder()
                .userId(1L)
                .garment(validGarment)
                .size(Size.M)
                .conservationStatus(ConservationStatus.LIKE_NEW)
                .price(BigDecimal.valueOf(50000.0))
                .build();
        pub.publish();

        BigDecimal newPrice = BigDecimal.valueOf(30000.0);
        pub.setStatus(PublicationStatus.SOLD);
        assertThrows(InvalidStateTransitionException.class,
                () -> pub.edit(validGarment, Size.M, ConservationStatus.LIKE_NEW, newPrice, null));

        pub.setStatus(PublicationStatus.RETIRED);
        assertThrows(InvalidStateTransitionException.class,
                () -> pub.edit(validGarment, Size.M, ConservationStatus.LIKE_NEW, newPrice, null));

        pub.setStatus(PublicationStatus.DONATED);
        assertThrows(InvalidStateTransitionException.class,
                () -> pub.edit(validGarment, Size.M, ConservationStatus.LIKE_NEW, newPrice, null));
    }

    @Test
    @DisplayName("Debe marcar como vendida y validar transiciones ilegales")
    void shouldManageMarkAsSold() {
        SecondHandPublication pub = SecondHandPublication.builder()
                .userId(1L)
                .garment(validGarment)
                .size(Size.M)
                .conservationStatus(ConservationStatus.LIKE_NEW)
                .price(BigDecimal.valueOf(50000.0))
                .build();
        pub.publish();

        pub.markAsSold();
        assertEquals(PublicationStatus.SOLD, pub.getStatus());

        assertThrows(InvalidStateTransitionException.class, pub::markAsSold);

        pub.setStatus(PublicationStatus.RETIRED);
        assertThrows(InvalidStateTransitionException.class, pub::markAsSold);

        pub.setStatus(PublicationStatus.IN_DONATION);
        assertThrows(InvalidStateTransitionException.class, pub::markAsSold);

        pub.setStatus(PublicationStatus.DONATED);
        assertThrows(InvalidStateTransitionException.class, pub::markAsSold);
    }

    @Test
    @DisplayName("Debe retirar publicación y validar transiciones ilegales")
    void shouldManageRetire() {
        SecondHandPublication pub = SecondHandPublication.builder()
                .userId(1L)
                .garment(validGarment)
                .size(Size.M)
                .conservationStatus(ConservationStatus.LIKE_NEW)
                .price(BigDecimal.valueOf(50000.0))
                .build();
        pub.publish();

        pub.retire();
        assertEquals(PublicationStatus.RETIRED, pub.getStatus());
        assertThrows(InvalidStateTransitionException.class, pub::retire);

        pub.setStatus(PublicationStatus.DONATED);
        assertThrows(InvalidStateTransitionException.class, pub::retire);
    }

    @Test
    @DisplayName("Debe gestionar ciclo hacia donación y donada correctamente")
    void shouldManageDonationLifecycle() {
        SecondHandPublication pub = SecondHandPublication.builder()
                .userId(1L)
                .garment(validGarment)
                .size(Size.M)
                .conservationStatus(ConservationStatus.GOOD_CONDITION)
                .price(BigDecimal.ZERO)
                .esDonacion(true)
                .build();
        pub.publish();

        pub.markForDonation();
        assertEquals(PublicationStatus.IN_DONATION, pub.getStatus());

        // No puede destinarse a donacion si ya no esta AVAILABLE
        assertThrows(InvalidStateTransitionException.class, pub::markForDonation);

        pub.markAsDonated();
        assertEquals(PublicationStatus.DONATED, pub.getStatus());

        // No puede marcarse como donada si ya no esta IN_DONATION
        assertThrows(InvalidStateTransitionException.class, pub::markAsDonated);
    }
}
