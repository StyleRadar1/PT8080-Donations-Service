package edu.dosw.styleradar.donaciones.domain.model;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.dosw.styleradar.donaciones.domain.event.DonacionEntregadaEvent;
import edu.dosw.styleradar.donaciones.domain.event.PublicacionCreadaEvent;
import edu.dosw.styleradar.donaciones.domain.event.PublicacionVendidaEvent;
import edu.dosw.styleradar.donaciones.domain.event.RecogidaProgramadaEvent;
import edu.dosw.styleradar.donaciones.domain.event.SolicitudDonacionCreadaEvent;
import edu.dosw.styleradar.donaciones.domain.event.SolicitudDonacionDecididaEvent;
import edu.dosw.styleradar.donaciones.domain.exception.ConflictoDominioException;
import edu.dosw.styleradar.donaciones.domain.exception.DomainException;
import edu.dosw.styleradar.donaciones.domain.exception.PublicacionInvalidaException;
import edu.dosw.styleradar.donaciones.domain.exception.RecursoNoEncontradoException;
import edu.dosw.styleradar.donaciones.domain.exception.SolicitudDonacionInvalidaException;
import edu.dosw.styleradar.donaciones.domain.exception.TransicionEstadoInvalidaException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Enums, Eventos y Excepciones del Dominio")
class EnumsAndEventsTest {

    @Test
    @DisplayName("Debe verificar todos los valores de los enums de dominio")
    void debeVerificarEnums() {
        assertEquals(5, EstadoPublicacion.values().length);
        assertEquals(4, EstadoConservacion.values().length);
        assertEquals(7, Talla.values().length);
        assertEquals(5, TipoPrenda.values().length);
        assertEquals(5, Estilo.values().length);
        assertEquals(5, EstadoSolicitudDonacion.values().length);

        assertNotNull(EstadoPublicacion.valueOf("DISPONIBLE"));
        assertNotNull(EstadoConservacion.valueOf("BUEN_ESTADO"));
        assertNotNull(Talla.valueOf("M"));
        assertNotNull(TipoPrenda.valueOf("SUPERIOR"));
        assertNotNull(Estilo.valueOf("CASUAL"));
        assertNotNull(EstadoSolicitudDonacion.valueOf("PENDIENTE"));
    }

    @Test
    @DisplayName("Debe instanciar y verificar eventos de dominio")
    void debeInstanciarEventos() {
        Instant ahora = Instant.now();

        PublicacionCreadaEvent ev1 = new PublicacionCreadaEvent(1L, 2L, 50000.0);
        assertEquals(1L, ev1.publicacionId());
        assertEquals(2L, ev1.usuarioId());
        assertEquals(50000.0, ev1.precio());
        assertNotNull(ev1.ocurridoEn());

        PublicacionVendidaEvent ev2 = new PublicacionVendidaEvent(1L, 2L);
        assertEquals(1L, ev2.publicacionId());
        assertNotNull(ev2.ocurridoEn());

        SolicitudDonacionCreadaEvent ev3 = new SolicitudDonacionCreadaEvent(10L, 1L, 2L, 3L);
        assertEquals(10L, ev3.solicitudId());
        assertEquals(1L, ev3.publicacionId());

        SolicitudDonacionDecididaEvent ev4 = new SolicitudDonacionDecididaEvent(10L, 3L, true, null);
        assertTrue(ev4.aprobada());
        assertNull(ev4.motivoRechazo());

        RecogidaProgramadaEvent ev5 = new RecogidaProgramadaEvent(10L, "Sede Central", ahora, ahora.plusSeconds(3600));
        assertEquals("Sede Central", ev5.puntoEntrega());

        DonacionEntregadaEvent ev6 = new DonacionEntregadaEvent(10L, 1L, 3L);
        assertEquals(10L, ev6.solicitudId());
    }

    @Test
    @DisplayName("Debe instanciar y verificar excepciones del dominio")
    void debeInstanciarExcepciones() {
        DomainException ex1 = new DomainException("Error", new RuntimeException("Causa"));
        assertEquals("Error", ex1.getMessage());
        assertNotNull(ex1.getCause());

        PublicacionInvalidaException ex2 = new PublicacionInvalidaException("Pub invalida");
        assertEquals("Pub invalida", ex2.getMessage());

        TransicionEstadoInvalidaException ex3 = new TransicionEstadoInvalidaException("Transición invalida");
        assertEquals("Transición invalida", ex3.getMessage());

        SolicitudDonacionInvalidaException ex4 = new SolicitudDonacionInvalidaException("Solicitud invalida");
        assertEquals("Solicitud invalida", ex4.getMessage());

        RecursoNoEncontradoException ex5 = new RecursoNoEncontradoException("No encontrado");
        assertEquals("No encontrado", ex5.getMessage());

        ConflictoDominioException ex6 = new ConflictoDominioException("Conflicto");
        assertEquals("Conflicto", ex6.getMessage());
    }

    @Test
    @DisplayName("Debe verificar getters y setters de BusquedaPublicacionesCriteria")
    void debeVerificarCriteria() {
        BusquedaPublicacionesCriteria criteria = BusquedaPublicacionesCriteria.builder()
                .terminoBusqueda("Chaqueta")
                .tipo(TipoPrenda.SUPERIOR)
                .talla(Talla.L)
                .color("Negro")
                .marca("Zara")
                .estilo(Estilo.URBANO)
                .estadoConservacion(EstadoConservacion.COMO_NUEVO)
                .precioMin(20000.0)
                .precioMax(80000.0)
                .estado(EstadoPublicacion.DISPONIBLE)
                .build();

        assertEquals("Chaqueta", criteria.getTerminoBusqueda());
        assertEquals(TipoPrenda.SUPERIOR, criteria.getTipo());
        assertEquals(Talla.L, criteria.getTalla());
        assertEquals("Negro", criteria.getColor());
        assertEquals("Zara", criteria.getMarca());
        assertEquals(Estilo.URBANO, criteria.getEstilo());
        assertEquals(EstadoConservacion.COMO_NUEVO, criteria.getEstadoConservacion());
        assertEquals(20000.0, criteria.getPrecioMin());
        assertEquals(80000.0, criteria.getPrecioMax());
        assertEquals(EstadoPublicacion.DISPONIBLE, criteria.getEstado());
    }
}
