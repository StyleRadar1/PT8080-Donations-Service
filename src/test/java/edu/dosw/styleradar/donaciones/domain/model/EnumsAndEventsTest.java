package edu.dosw.styleradar.donaciones.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.dosw.styleradar.donaciones.domain.event.DonationDeliveredEvent;
import edu.dosw.styleradar.donaciones.domain.event.DonationRequestCreatedEvent;
import edu.dosw.styleradar.donaciones.domain.event.DonationRequestDecidedEvent;
import edu.dosw.styleradar.donaciones.domain.event.PickupScheduledEvent;
import edu.dosw.styleradar.donaciones.domain.event.PublicationCreatedEvent;
import edu.dosw.styleradar.donaciones.domain.event.PublicationSoldEvent;
import edu.dosw.styleradar.donaciones.domain.exception.DomainConflictException;
import edu.dosw.styleradar.donaciones.domain.exception.DomainException;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidDonationRequestException;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidPublicationException;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidStateTransitionException;
import edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para Enums, Eventos y Excepciones de dominio.
 */
@DisplayName("Pruebas Unitarias - Enums, Eventos y Excepciones de Dominio")
class EnumsAndEventsTest {

    @Test
    @DisplayName("Debe verificar todos los valores de los enums de dominio incluyendo las 9 categorías y 18 tallas")
    void shouldVerifyAllEnumValues() {
        assertEquals(5, PublicationStatus.values().length);
        assertEquals(4, ConservationStatus.values().length);
        assertEquals(18, Size.values().length);
        assertEquals(9, GarmentType.values().length);
        assertEquals(5, Style.values().length);
        assertEquals(5, DonationRequestStatus.values().length);

        assertNotNull(PublicationStatus.valueOf("AVAILABLE"));
        assertNotNull(ConservationStatus.valueOf("GOOD_CONDITION"));
        assertNotNull(Size.valueOf("M"));
        assertNotNull(Size.valueOf("TALLA_28"));
        assertNotNull(Size.valueOf("UNICA"));
        assertNotNull(GarmentType.valueOf("CAMISAS"));
        assertNotNull(GarmentType.valueOf("SUETERES"));
        assertNotNull(GarmentType.valueOf("ACCESORIOS"));
        assertNotNull(Style.valueOf("CASUAL"));
        assertNotNull(DonationRequestStatus.valueOf("PENDING"));
    }

    @Test
    @DisplayName("Debe resolver GarmentType desde los 9 valores oficiales, alias previos, tildes, nulos e inválidos")
    void shouldResolveGarmentTypeFromString() {
        assertEquals(GarmentType.CAMISAS, GarmentType.fromString("Camisas"));
        assertEquals(GarmentType.CAMISETAS, GarmentType.fromString("Camisetas"));
        assertEquals(GarmentType.SUETERES, GarmentType.fromString("Suéteres"));
        assertEquals(GarmentType.SUETERES, GarmentType.fromString("sueter"));
        assertEquals(GarmentType.CHAQUETAS, GarmentType.fromString("Chaquetas"));
        assertEquals(GarmentType.PANTALONES, GarmentType.fromString("Pantalones"));
        assertEquals(GarmentType.FALDAS, GarmentType.fromString("Faldas"));
        assertEquals(GarmentType.VESTIDOS, GarmentType.fromString("Vestidos"));
        assertEquals(GarmentType.CALZADO, GarmentType.fromString("Calzado"));
        assertEquals(GarmentType.ACCESORIOS, GarmentType.fromString("Accesorios"));

        // Alias previos para retrocompatibilidad
        assertEquals(GarmentType.CAMISAS, GarmentType.fromString("UPPER"));
        assertEquals(GarmentType.PANTALONES, GarmentType.fromString("LOWER"));
        assertEquals(GarmentType.VESTIDOS, GarmentType.fromString("DRESS"));
        assertEquals(GarmentType.VESTIDOS, GarmentType.fromString("FULL_BODY"));
        assertEquals(GarmentType.CALZADO, GarmentType.fromString("FOOTWEAR"));
        assertEquals(GarmentType.ACCESORIOS, GarmentType.fromString("ACCESSORY"));
        assertEquals("Chaquetas", GarmentType.CHAQUETAS.getLabel());

        assertNull(GarmentType.fromString(null));
        assertNull(GarmentType.fromString("   "));
        assertThrows(IllegalArgumentException.class, () -> GarmentType.fromString("SOMBRERO_DESCONOCIDO"));
    }

    @Test
    @DisplayName("Debe resolver Size desde las 18 tallas oficiales, alias previos, nulos e inválidos")
    void shouldResolveSizeFromString() {
        assertEquals(Size.XS, Size.fromString("XS"));
        assertEquals(Size.S, Size.fromString("S"));
        assertEquals(Size.M, Size.fromString("M"));
        assertEquals(Size.L, Size.fromString("L"));
        assertEquals(Size.XL, Size.fromString("XL"));
        assertEquals(Size.XXL, Size.fromString("XXL"));
        assertEquals(Size.TALLA_28, Size.fromString("28"));
        assertEquals(Size.TALLA_30, Size.fromString("30"));
        assertEquals(Size.TALLA_32, Size.fromString("32"));
        assertEquals(Size.TALLA_34, Size.fromString("34"));
        assertEquals(Size.TALLA_36, Size.fromString("36"));
        assertEquals(Size.TALLA_37, Size.fromString("37"));
        assertEquals(Size.TALLA_38, Size.fromString("38"));
        assertEquals(Size.TALLA_39, Size.fromString("39"));
        assertEquals(Size.TALLA_40, Size.fromString("40"));
        assertEquals(Size.TALLA_41, Size.fromString("41"));
        assertEquals(Size.TALLA_42, Size.fromString("42"));
        assertEquals(Size.UNICA, Size.fromString("Unica"));
        assertEquals(Size.UNICA, Size.fromString("Única"));
        assertEquals(Size.UNICA, Size.fromString("ONE_SIZE"));
        assertEquals(Size.TALLA_28, Size.fromString("TALLA_28"));
        assertEquals("38", Size.TALLA_38.getValue());
        assertEquals("Unica", Size.UNICA.getValue());

        assertNull(Size.fromString(null));
        assertNull(Size.fromString("  "));
        assertThrows(IllegalArgumentException.class, () -> Size.fromString("99"));
    }

    @Test
    @DisplayName("Debe instanciar y verificar eventos de dominio")
    void shouldInstantiateAndVerifyDomainEvents() {
        LocalDateTime now = LocalDateTime.now();

        PublicationCreatedEvent ev1 = new PublicationCreatedEvent(1L, 2L, BigDecimal.valueOf(50000.0));
        assertEquals(1L, ev1.publicationId());
        assertEquals(2L, ev1.userId());
        assertEquals(BigDecimal.valueOf(50000.0), ev1.price());
        assertNotNull(ev1.occurredOn());

        PublicationSoldEvent ev2 = new PublicationSoldEvent(1L, 2L);
        assertEquals(1L, ev2.publicationId());
        assertNotNull(ev2.occurredOn());
        assertTrue(ev2.interesadosIds().isEmpty());

        PublicationSoldEvent ev2Full = new PublicationSoldEvent(1L, 2L, 3L, java.util.List.of(4L, 5L, 2L, 3L, 4L));
        assertEquals(java.util.List.of(4L, 5L), ev2Full.interesadosIds());

        DonationRequestCreatedEvent ev3 = new DonationRequestCreatedEvent(10L, 1L, 2L, 3L);
        assertEquals(10L, ev3.requestId());
        assertEquals(1L, ev3.publicationId());
        assertNull(ev3.representanteId());

        DonationRequestCreatedEvent ev3WithRep = new DonationRequestCreatedEvent(10L, 1L, 2L, 3L, "PENDING", 77L);
        assertEquals(77L, ev3WithRep.representanteId());
        assertNotNull(ev3WithRep.occurredOn());

        Instant ts = Instant.parse("2026-11-17T10:00:00Z");
        DonationRequestCreatedEvent ev3Full = new DonationRequestCreatedEvent(10L, 1L, 2L, 3L, "APPROVED", ts, 88L);
        assertEquals(88L, ev3Full.representanteId());
        assertEquals(ts, ev3Full.occurredOn());

        DonationRequestDecidedEvent ev4 = new DonationRequestDecidedEvent(10L, 3L, true, null);
        assertTrue(ev4.approved());
        assertNull(ev4.rejectionReason());

        PickupScheduledEvent ev5 = new PickupScheduledEvent(10L, "Sede Central", now, now.plusHours(1));
        assertEquals("Sede Central", ev5.deliveryPoint());

        DonationDeliveredEvent ev6 = new DonationDeliveredEvent(10L, 1L, 3L);
        assertEquals(10L, ev6.requestId());
    }

    @Test
    @DisplayName("Debe instanciar y verificar excepciones de dominio")
    void shouldInstantiateAndVerifyDomainExceptions() {
        DomainException ex1 = new DomainException("Error", new RuntimeException("Causa"));
        assertEquals("Error", ex1.getMessage());
        assertNotNull(ex1.getCause());

        InvalidPublicationException ex2 = new InvalidPublicationException("Pub invalida");
        assertEquals("Pub invalida", ex2.getMessage());

        InvalidStateTransitionException ex3 = new InvalidStateTransitionException("Transición invalida");
        assertEquals("Transición invalida", ex3.getMessage());

        InvalidDonationRequestException ex4 = new InvalidDonationRequestException("Solicitud invalida");
        assertEquals("Solicitud invalida", ex4.getMessage());

        ResourceNotFoundException ex5 = new ResourceNotFoundException("No encontrado");
        assertEquals("No encontrado", ex5.getMessage());

        DomainConflictException ex6 = new DomainConflictException("Conflicto");
        assertEquals("Conflicto", ex6.getMessage());
    }

    @Test
    @DisplayName("Debe verificar getters y setters de PublicationSearchCriteria")
    void shouldVerifyPublicationSearchCriteria() {
        PublicationSearchCriteria criteria = PublicationSearchCriteria.builder()
                .searchTerm("Chaqueta")
                .type(GarmentType.CHAQUETAS)
                .size(Size.L)
                .color("Negro")
                .brand("Zara")
                .style(Style.URBAN)
                .conservationStatus(ConservationStatus.LIKE_NEW)
                .minPrice(BigDecimal.valueOf(20000.0))
                .maxPrice(BigDecimal.valueOf(80000.0))
                .status(PublicationStatus.AVAILABLE)
                .build();

        assertEquals("Chaqueta", criteria.getSearchTerm());
        assertEquals(GarmentType.CHAQUETAS, criteria.getType());
        assertEquals(Size.L, criteria.getSize());
        assertEquals("Negro", criteria.getColor());
        assertEquals("Zara", criteria.getBrand());
        assertEquals(Style.URBAN, criteria.getStyle());
        assertEquals(ConservationStatus.LIKE_NEW, criteria.getConservationStatus());
        assertEquals(BigDecimal.valueOf(20000.0), criteria.getMinPrice());
        assertEquals(BigDecimal.valueOf(80000.0), criteria.getMaxPrice());
        assertEquals(PublicationStatus.AVAILABLE, criteria.getStatus());
    }
}
