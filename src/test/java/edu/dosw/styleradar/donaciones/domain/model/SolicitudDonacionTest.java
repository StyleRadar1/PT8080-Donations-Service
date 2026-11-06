package edu.dosw.styleradar.donaciones.domain.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.dosw.styleradar.donaciones.domain.exception.SolicitudDonacionInvalidaException;
import edu.dosw.styleradar.donaciones.domain.exception.TransicionEstadoInvalidaException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Entidad SolicitudDonacion")
class SolicitudDonacionTest {

    @Test
    @DisplayName("Debe iniciar solicitud con estado PENDIENTE e invariantes válidas")
    void debeIniciarSolicitudExitosamente() {
        SolicitudDonacion solicitud = SolicitudDonacion.builder()
                .publicacionId(100L)
                .donanteId(1L)
                .fundacionId(50L)
                .mensaje("Ropa en perfecto estado para niños")
                .build();

        solicitud.iniciarSolicitud();

        assertEquals(EstadoSolicitudDonacion.PENDIENTE, solicitud.getEstado());
        assertNotNull(solicitud.getFechaSolicitud());
        assertTrue(solicitud.getEstado().isEnCurso());
        assertFalse(solicitud.getEstado().isFinalizada());
    }

    @Test
    @DisplayName("Debe fallar al iniciar solicitud con campos obligatorios faltantes")
    void debeFallarPorInvariantesFaltantes() {
        SolicitudDonacion solicitud = SolicitudDonacion.builder().build();
        assertThrows(SolicitudDonacionInvalidaException.class, solicitud::iniciarSolicitud);

        solicitud.setPublicacionId(10L);
        assertThrows(SolicitudDonacionInvalidaException.class, solicitud::iniciarSolicitud);

        solicitud.setDonanteId(2L);
        assertThrows(SolicitudDonacionInvalidaException.class, solicitud::iniciarSolicitud);
    }

    @Test
    @DisplayName("Debe aprobar y rechazar solicitud en estado PENDIENTE")
    void debeAprobarYRechazarSolicitud() {
        SolicitudDonacion solAprobar = SolicitudDonacion.builder()
                .publicacionId(10L)
                .donanteId(2L)
                .fundacionId(3L)
                .build();
        solAprobar.iniciarSolicitud();
        solAprobar.aprobar();

        assertEquals(EstadoSolicitudDonacion.APROBADA, solAprobar.getEstado());
        assertNotNull(solAprobar.getFechaDecision());
        assertTrue(solAprobar.getEstado().isEnCurso());

        // Intentar aprobar nuevamente debe lanzar excepción
        assertThrows(TransicionEstadoInvalidaException.class, solAprobar::aprobar);

        // Rechazo
        SolicitudDonacion solRechazar = SolicitudDonacion.builder()
                .publicacionId(11L)
                .donanteId(2L)
                .fundacionId(3L)
                .build();
        solRechazar.iniciarSolicitud();

        assertThrows(SolicitudDonacionInvalidaException.class, () -> solRechazar.rechazar("   "));
        assertThrows(SolicitudDonacionInvalidaException.class, () -> solRechazar.rechazar(null));

        solRechazar.rechazar("No recibimos calzado en este momento");
        assertEquals(EstadoSolicitudDonacion.RECHAZADA, solRechazar.getEstado());
        assertEquals("No recibimos calzado en este momento", solRechazar.getMotivoRechazo());
        assertNotNull(solRechazar.getFechaDecision());
        assertTrue(solRechazar.getEstado().isFinalizada());
        assertFalse(solRechazar.getEstado().isEnCurso());

        // No se puede rechazar si ya no está PENDIENTE
        assertThrows(TransicionEstadoInvalidaException.class, () -> solRechazar.rechazar("Otro motivo"));
    }

    @Test
    @DisplayName("Debe programar recogida cuando la solicitud está aprobada")
    void debeProgramarRecogidaCorrectamente() {
        SolicitudDonacion solicitud = SolicitudDonacion.builder()
                .publicacionId(10L)
                .donanteId(2L)
                .fundacionId(3L)
                .build();
        solicitud.iniciarSolicitud();

        // No puede programar si aún está PENDIENTE
        Instant inicio = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant fin = inicio.plus(2, ChronoUnit.HOURS);
        assertThrows(TransicionEstadoInvalidaException.class,
                () -> solicitud.programarRecogida("Sede Norte", inicio, fin));

        solicitud.aprobar();

        // Validaciones de franja y punto
        assertThrows(SolicitudDonacionInvalidaException.class,
                () -> solicitud.programarRecogida(" ", inicio, fin));
        assertThrows(SolicitudDonacionInvalidaException.class,
                () -> solicitud.programarRecogida("Sede Norte", null, fin));
        assertThrows(SolicitudDonacionInvalidaException.class,
                () -> solicitud.programarRecogida("Sede Norte", fin, inicio)); // fin antes de inicio

        solicitud.programarRecogida("Carrera 7 # 120-10, Bogotá", inicio, fin);

        assertEquals(EstadoSolicitudDonacion.EN_RECOGIDA, solicitud.getEstado());
        assertEquals("Carrera 7 # 120-10, Bogotá", solicitud.getPuntoEntrega());
        assertEquals(inicio, solicitud.getFranjaInicio());
        assertEquals(fin, solicitud.getFranjaFin());
    }

    @Test
    @DisplayName("Debe confirmar entrega cuando la solicitud está en recogida")
    void debeConfirmarEntregaCorrectamente() {
        SolicitudDonacion solicitud = SolicitudDonacion.builder()
                .publicacionId(10L)
                .donanteId(2L)
                .fundacionId(3L)
                .build();
        solicitud.iniciarSolicitud();

        // No puede confirmar entrega en estado PENDIENTE
        assertThrows(TransicionEstadoInvalidaException.class, solicitud::confirmarEntrega);

        solicitud.aprobar();
        // No puede confirmar entrega en estado APROBADA
        assertThrows(TransicionEstadoInvalidaException.class, solicitud::confirmarEntrega);

        Instant inicio = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant fin = inicio.plus(2, ChronoUnit.HOURS);
        solicitud.programarRecogida("Punto Acopio Central", inicio, fin);

        solicitud.confirmarEntrega();

        assertEquals(EstadoSolicitudDonacion.ENTREGADA, solicitud.getEstado());
        assertNotNull(solicitud.getFechaRecepcion());
        assertTrue(solicitud.getEstado().isFinalizada());

        // No puede volver a confirmar si ya está ENTREGADA
        assertThrows(TransicionEstadoInvalidaException.class, solicitud::confirmarEntrega);
    }
}
