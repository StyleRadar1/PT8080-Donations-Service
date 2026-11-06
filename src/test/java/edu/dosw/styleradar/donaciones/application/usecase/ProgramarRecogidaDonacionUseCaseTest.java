package edu.dosw.styleradar.donaciones.application.usecase;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.donaciones.domain.event.RecogidaProgramadaEvent;
import edu.dosw.styleradar.donaciones.domain.exception.RecursoNoEncontradoException;
import edu.dosw.styleradar.donaciones.domain.exception.SolicitudDonacionInvalidaException;
import edu.dosw.styleradar.donaciones.domain.model.EstadoSolicitudDonacion;
import edu.dosw.styleradar.donaciones.domain.model.SolicitudDonacion;
import edu.dosw.styleradar.donaciones.domain.port.in.ProgramarRecogidaCommand;
import edu.dosw.styleradar.donaciones.domain.port.out.DonacionEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.SolicitudDonacionRepositoryPort;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - ProgramarRecogidaDonacionUseCaseImpl")
class ProgramarRecogidaDonacionUseCaseTest {

    @Mock
    private SolicitudDonacionRepositoryPort solicitudRepositoryPort;

    @Mock
    private DonacionEventPublisherPort eventPublisherPort;

    @InjectMocks
    private ProgramarRecogidaDonacionUseCaseImpl programarRecogidaDonacionUseCase;

    private SolicitudDonacion solicitud;
    private Instant inicio;
    private Instant fin;

    @BeforeEach
    void setUp() {
        solicitud = SolicitudDonacion.builder()
                .id(10L)
                .donanteId(1L)
                .fundacionId(20L)
                .estado(EstadoSolicitudDonacion.APROBADA)
                .build();

        inicio = Instant.now().plus(1, ChronoUnit.DAYS);
        fin = inicio.plus(2, ChronoUnit.HOURS);
    }

    @Test
    @DisplayName("Debe programar recogida exitosamente por el donante y emitir evento")
    void debeProgramarRecogidaPorDonante() {
        ProgramarRecogidaCommand command = new ProgramarRecogidaCommand(
                1L, 10L, "Calle 100 # 15-20, Bogotá", inicio, fin
        );

        when(solicitudRepositoryPort.buscarPorId(10L)).thenReturn(Optional.of(solicitud));
        when(solicitudRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        SolicitudDonacion resultado = programarRecogidaDonacionUseCase.programarRecogida(command);

        assertEquals(EstadoSolicitudDonacion.EN_RECOGIDA, resultado.getEstado());
        assertEquals("Calle 100 # 15-20, Bogotá", resultado.getPuntoEntrega());
        verify(solicitudRepositoryPort).guardar(solicitud);
        verify(eventPublisherPort).publicarEvento(any(RecogidaProgramadaEvent.class));
    }

    @Test
    @DisplayName("Debe programar recogida exitosamente por la fundación")
    void debeProgramarRecogidaPorFundacion() {
        ProgramarRecogidaCommand command = new ProgramarRecogidaCommand(
                20L, 10L, "Sede Fundación", inicio, fin
        );

        when(solicitudRepositoryPort.buscarPorId(10L)).thenReturn(Optional.of(solicitud));
        when(solicitudRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        SolicitudDonacion resultado = programarRecogidaDonacionUseCase.programarRecogida(command);

        assertEquals(EstadoSolicitudDonacion.EN_RECOGIDA, resultado.getEstado());
    }

    @Test
    @DisplayName("Debe fallar si la solicitud no existe")
    void debeFallarSiSolicitudNoExiste() {
        ProgramarRecogidaCommand command = new ProgramarRecogidaCommand(1L, 999L, "Punto", inicio, fin);
        when(solicitudRepositoryPort.buscarPorId(999L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> programarRecogidaDonacionUseCase.programarRecogida(command));
    }

    @Test
    @DisplayName("Debe fallar si el usuario no participa en la solicitud")
    void debeFallarSiUsuarioNoEsParticipante() {
        ProgramarRecogidaCommand command = new ProgramarRecogidaCommand(888L, 10L, "Punto", inicio, fin);
        when(solicitudRepositoryPort.buscarPorId(10L)).thenReturn(Optional.of(solicitud));

        assertThrows(SolicitudDonacionInvalidaException.class,
                () -> programarRecogidaDonacionUseCase.programarRecogida(command));
    }
}
