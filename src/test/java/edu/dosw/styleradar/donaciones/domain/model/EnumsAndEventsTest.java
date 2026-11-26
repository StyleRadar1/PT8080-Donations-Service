package edu.dosw.styleradar.donaciones.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.dosw.styleradar.donaciones.domain.event.DonationDeliveredEvent;
import edu.dosw.styleradar.donaciones.domain.event.DonationDeliveredStatisticsEvent;
import edu.dosw.styleradar.donaciones.domain.event.DonationRequestCreatedEvent;
import edu.dosw.styleradar.donaciones.domain.event.DonationRequestDecidedEvent;
import edu.dosw.styleradar.donaciones.domain.event.PickupScheduledEvent;
import edu.dosw.styleradar.donaciones.domain.event.PublicationCreatedEvent;
import edu.dosw.styleradar.donaciones.domain.event.PublicationSoldEvent;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging.event.DonationCompletedStatisticsIntegrationEvent;
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
        assertEquals(GarmentType.SUETERES, GarmentType.fromString("Saco"));
        assertEquals(GarmentType.SUETERES, GarmentType.fromString("Sacos"));
        assertEquals(GarmentType.VESTIDOS, GarmentType.fromString("Conjunto"));
        assertEquals(GarmentType.VESTIDOS, GarmentType.fromString("Conjuntos"));
        assertEquals(GarmentType.CALZADO, GarmentType.fromString("FOOTWEAR"));
        assertEquals(GarmentType.ACCESORIOS, GarmentType.fromString("ACCESSORY"));
        assertEquals("Chaquetas", GarmentType.CHAQUETAS.getLabel());

        assertNull(GarmentType.fromString(null));
        assertNull(GarmentType.fromString("   "));
        assertThrows(IllegalArgumentException.class, () -> GarmentType.fromString("SOMBRERO_DESCONOCIDO"));
    }

    @Test
    @DisplayName("Debe resolver ConservationStatus desde valores oficiales, alias en español, nulos e inválidos")
    void shouldResolveConservationStatusFromString() {
        assertEquals(ConservationStatus.NEW_WITH_TAGS, ConservationStatus.fromString("NEW_WITH_TAGS"));
        assertEquals(ConservationStatus.NEW_WITH_TAGS, ConservationStatus.fromString("Nuevo con etiquetas"));
        assertEquals(ConservationStatus.LIKE_NEW, ConservationStatus.fromString("LIKE_NEW"));
        assertEquals(ConservationStatus.LIKE_NEW, ConservationStatus.fromString("Como nuevo"));
        assertEquals(ConservationStatus.LIKE_NEW, ConservationStatus.fromString("COMO_NUEVO"));
        assertEquals(ConservationStatus.GOOD_CONDITION, ConservationStatus.fromString("GOOD_CONDITION"));
        assertEquals(ConservationStatus.GOOD_CONDITION, ConservationStatus.fromString("Buen estado"));
        assertEquals(ConservationStatus.GOOD_CONDITION, ConservationStatus.fromString("MUY_BUENO"));
        assertEquals(ConservationStatus.ACCEPTABLE, ConservationStatus.fromString("ACCEPTABLE"));
        assertEquals(ConservationStatus.ACCEPTABLE, ConservationStatus.fromString("Aceptable"));
        assertEquals(ConservationStatus.ACCEPTABLE, ConservationStatus.fromString("BUENO"));

        assertNull(ConservationStatus.fromString(null));
        assertNull(ConservationStatus.fromString("  "));
        assertThrows(IllegalArgumentException.class, () -> ConservationStatus.fromString("ROTO_DESCONOCIDO"));
    }

    @Test
    @DisplayName("Debe resolver Size desde las 18 tallas oficiales")
    void shouldResolveSizeFromString() {
        java.util.Map<String, Size> officialSizes = java.util.Map.ofEntries(
                java.util.Map.entry("XS", Size.XS),
                java.util.Map.entry("S", Size.S),
                java.util.Map.entry("M", Size.M),
                java.util.Map.entry("L", Size.L),
                java.util.Map.entry("XL", Size.XL),
                java.util.Map.entry("XXL", Size.XXL),
                java.util.Map.entry("28", Size.TALLA_28),
                java.util.Map.entry("30", Size.TALLA_30),
                java.util.Map.entry("32", Size.TALLA_32),
                java.util.Map.entry("34", Size.TALLA_34),
                java.util.Map.entry("36", Size.TALLA_36),
                java.util.Map.entry("37", Size.TALLA_37),
                java.util.Map.entry("38", Size.TALLA_38),
                java.util.Map.entry("39", Size.TALLA_39),
                java.util.Map.entry("40", Size.TALLA_40),
                java.util.Map.entry("41", Size.TALLA_41),
                java.util.Map.entry("42", Size.TALLA_42),
                java.util.Map.entry("Unica", Size.UNICA)
        );
        officialSizes.forEach((raw, expected) -> assertEquals(expected, Size.fromString(raw)));
        assertEquals("38", Size.TALLA_38.getValue());
        assertEquals("Unica", Size.UNICA.getValue());
    }

    @Test
    @DisplayName("Debe resolver alias previos de Size y manejar nulos e inválidos")
    void shouldResolveSizeAliasesAndRejectInvalid() {
        assertEquals(Size.UNICA, Size.fromString("Única"));
        assertEquals(Size.UNICA, Size.fromString("ONE_SIZE"));
        assertEquals(Size.TALLA_28, Size.fromString("TALLA_28"));
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

    @Test
    @DisplayName("Debe construir DonationCompletedStatisticsIntegrationEvent desde eventos válidos, nulos y con eventId vacío")
    void shouldCoverDonationCompletedStatisticsIntegrationEventBranches() {
        var fromNullStats = DonationCompletedStatisticsIntegrationEvent.from((DonationDeliveredStatisticsEvent) null);
        assertNotNull(fromNullStats.getEventId());
        assertNotNull(fromNullStats.getOccurredAt());
        assertNull(fromNullStats.getData().getFundacionId());
        assertEquals(1, fromNullStats.getData().getCantidad());

        var fromNullDelivered = DonationCompletedStatisticsIntegrationEvent.from((DonationDeliveredEvent) null);
        assertNotNull(fromNullDelivered.getEventId());
        assertNull(fromNullDelivered.getData().getFundacionId());

        var fromDelivered = DonationCompletedStatisticsIntegrationEvent.from(new DonationDeliveredEvent(10L, 1L, 3L));
        assertEquals("3", fromDelivered.getData().getFundacionId());
        assertNotNull(fromDelivered.getOccurredAt());

        var fromBlankEventId = DonationCompletedStatisticsIntegrationEvent.from(
                new DonationDeliveredStatisticsEvent(10L, 7L, null, "   "));
        assertNotNull(fromBlankEventId.getEventId());
        assertFalse(fromBlankEventId.getEventId().isBlank());
        assertEquals("7", fromBlankEventId.getData().getFundacionId());
    }
}
