package edu.dosw.styleradar.donaciones.domain.model;

import java.math.BigDecimal;
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
    @DisplayName("Debe verificar todos los valores de los enums de dominio")
    void shouldVerifyAllEnumValues() {
        assertEquals(5, PublicationStatus.values().length);
        assertEquals(4, ConservationStatus.values().length);
        assertEquals(7, Size.values().length);
        assertEquals(5, GarmentType.values().length);
        assertEquals(5, Style.values().length);
        assertEquals(5, DonationRequestStatus.values().length);

        assertNotNull(PublicationStatus.valueOf("AVAILABLE"));
        assertNotNull(ConservationStatus.valueOf("GOOD_CONDITION"));
        assertNotNull(Size.valueOf("M"));
        assertNotNull(GarmentType.valueOf("UPPER"));
        assertNotNull(Style.valueOf("CASUAL"));
        assertNotNull(DonationRequestStatus.valueOf("PENDING"));
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

        DonationRequestCreatedEvent ev3 = new DonationRequestCreatedEvent(10L, 1L, 2L, 3L);
        assertEquals(10L, ev3.requestId());
        assertEquals(1L, ev3.publicationId());

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
                .type(GarmentType.UPPER)
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
        assertEquals(GarmentType.UPPER, criteria.getType());
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
