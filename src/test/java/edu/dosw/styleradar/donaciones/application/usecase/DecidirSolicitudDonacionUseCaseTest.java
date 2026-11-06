package edu.dosw.styleradar.donaciones.application.usecase;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.donaciones.domain.event.SolicitudDonacionDecididaEvent;
import edu.dosw.styleradar.donaciones.domain.exception.RecursoNoEncontradoException;
import edu.dosw.styleradar.donaciones.domain.exception.SolicitudDonacionInvalidaException;
import edu.dosw.styleradar.donaciones.domain.model.EstadoSolicitudDonacion;
import edu.dosw.styleradar.donaciones.domain.model.SolicitudDonacion;
import edu.dosw.styleradar.donaciones.domain.port.out.DonacionEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.SolicitudDonacionRepositoryPort;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - DecidirSolicitudDonacionUseCaseImpl")
class DecidirSolicitudDonacionUseCaseTest {

    @Mock
    private SolicitudDonacionRepositoryPort solicitudRepositoryPort;

    @Mock
    private DonacionEventPublisherPort eventPublisherPort;

    @InjectMocks
    private DecidirSolicitudDonacionUseCaseImpl decidirSolicitudDonacionUseCase;

    private SolicitudDonacion solicitud;

    @BeforeEach
    void setUp() {
        solicitud = SolicitudDonacion.builder()
                .id(10L)
                .publicacionId(100L)
                .donanteId(1L)
                .fundacionId(20L)
                .estado(EstadoSolicitudDonacion.PENDIENTE)
                .build();
    }

    @Test
    @DisplayName("Debe aprobar solicitud exitosamente")
    void debeAprobarSolicitudExitosamente() {
        when(solicitudRepositoryPort.buscarPorId(10L)).thenReturn(Optional.of(solicitud));
        when(solicitudRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        SolicitudDonacion resultado = decidirSolicitudDonacionUseCase.decidir(20L, 10L, true, null);

        assertEquals(EstadoSolicitudDonacion.APROBADA, resultado.getEstado());
        verify(solicitudRepositoryPort).guardar(solicitud);
        verify(eventPublisherPort).publicarEvento(any(SolicitudDonacionDecididaEvent.class));
    }

    @Test
    @DisplayName("Debe rechazar solicitud con motivo y publicar evento")
    void debeRechazarSolicitudExitosamente() {
        when(solicitudRepositoryPort.buscarPorId(10L)).thenReturn(Optional.of(solicitud));
        when(solicitudRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        SolicitudDonacion resultado = decidirSolicitudDonacionUseCase.decidir(20L, 10L, false, "Capacidad colmada");

        assertEquals(EstadoSolicitudDonacion.RECHAZADA, resultado.getEstado());
        assertEquals("Capacidad colmada", resultado.getMotivoRechazo());
        verify(eventPublisherPort).publicarEvento(any(SolicitudDonacionDecididaEvent.class));
    }

    @Test
    @DisplayName("Debe fallar si la solicitud no existe")
    void debeFallarSiSolicitudNoExiste() {
        when(solicitudRepositoryPort.buscarPorId(999L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> decidirSolicitudDonacionUseCase.decidir(20L, 999L, true, null));
    }

    @Test
    @DisplayName("Debe fallar si la solicitud no corresponde a la fundación")
    void debeFallarSiFundacionNoCoincide() {
        when(solicitudRepositoryPort.buscarPorId(10L)).thenReturn(Optional.of(solicitud));

        assertThrows(SolicitudDonacionInvalidaException.class,
                () -> decidirSolicitudDonacionUseCase.decidir(999L, 10L, true, null));
    }
}
