package edu.dosw.styleradar.donaciones.application.usecase;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.donaciones.domain.event.DonacionEntregadaEvent;
import edu.dosw.styleradar.donaciones.domain.exception.RecursoNoEncontradoException;
import edu.dosw.styleradar.donaciones.domain.exception.SolicitudDonacionInvalidaException;
import edu.dosw.styleradar.donaciones.domain.model.EstadoPublicacion;
import edu.dosw.styleradar.donaciones.domain.model.EstadoSolicitudDonacion;
import edu.dosw.styleradar.donaciones.domain.model.PublicacionSegundaMano;
import edu.dosw.styleradar.donaciones.domain.model.SolicitudDonacion;
import edu.dosw.styleradar.donaciones.domain.port.out.DonacionEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicacionRepositoryPort;
import edu.dosw.styleradar.donaciones.domain.port.out.SolicitudDonacionRepositoryPort;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - ConfirmarRecepcionDonacionUseCaseImpl")
class ConfirmarRecepcionDonacionUseCaseTest {

    @Mock
    private SolicitudDonacionRepositoryPort solicitudRepositoryPort;

    @Mock
    private PublicacionRepositoryPort publicacionRepositoryPort;

    @Mock
    private DonacionEventPublisherPort eventPublisherPort;

    @InjectMocks
    private ConfirmarRecepcionDonacionUseCaseImpl confirmarRecepcionDonacionUseCase;

    private SolicitudDonacion solicitud;
    private PublicacionSegundaMano publicacion;

    @BeforeEach
    void setUp() {
        solicitud = SolicitudDonacion.builder()
                .id(10L)
                .publicacionId(100L)
                .fundacionId(20L)
                .estado(EstadoSolicitudDonacion.EN_RECOGIDA)
                .build();

        publicacion = PublicacionSegundaMano.builder()
                .id(100L)
                .usuarioId(1L)
                .estado(EstadoPublicacion.EN_DONACION)
                .build();
    }

    @Test
    @DisplayName("Debe confirmar recepción, marcar publicación DONADA y emitir evento")
    void debeConfirmarRecepcionExitosamente() {
        when(solicitudRepositoryPort.buscarPorId(10L)).thenReturn(Optional.of(solicitud));
        when(publicacionRepositoryPort.buscarPorId(100L)).thenReturn(Optional.of(publicacion));
        when(solicitudRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        SolicitudDonacion resultado = confirmarRecepcionDonacionUseCase.confirmarRecepcion(20L, 10L);

        assertEquals(EstadoSolicitudDonacion.ENTREGADA, resultado.getEstado());
        assertEquals(EstadoPublicacion.DONADA, publicacion.getEstado());

        verify(publicacionRepositoryPort).guardar(publicacion);
        verify(solicitudRepositoryPort).guardar(solicitud);
        verify(eventPublisherPort).publicarEvento(any(DonacionEntregadaEvent.class));
    }

    @Test
    @DisplayName("Debe confirmar recepción aunque la publicación no esté en el repositorio")
    void debeConfirmarRecepcionSinPublicacion() {
        when(solicitudRepositoryPort.buscarPorId(10L)).thenReturn(Optional.of(solicitud));
        when(publicacionRepositoryPort.buscarPorId(100L)).thenReturn(Optional.empty());
        when(solicitudRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        SolicitudDonacion resultado = confirmarRecepcionDonacionUseCase.confirmarRecepcion(20L, 10L);

        assertEquals(EstadoSolicitudDonacion.ENTREGADA, resultado.getEstado());
        verify(publicacionRepositoryPort, never()).guardar(any());
        verify(solicitudRepositoryPort).guardar(solicitud);
    }

    @Test
    @DisplayName("Debe fallar si la solicitud no existe")
    void debeFallarSiSolicitudNoExiste() {
        when(solicitudRepositoryPort.buscarPorId(999L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> confirmarRecepcionDonacionUseCase.confirmarRecepcion(20L, 999L));
    }

    @Test
    @DisplayName("Debe fallar si la solicitud no corresponde a la fundación")
    void debeFallarSiFundacionNoCoincide() {
        when(solicitudRepositoryPort.buscarPorId(10L)).thenReturn(Optional.of(solicitud));

        assertThrows(SolicitudDonacionInvalidaException.class,
                () -> confirmarRecepcionDonacionUseCase.confirmarRecepcion(888L, 10L));
    }
}
