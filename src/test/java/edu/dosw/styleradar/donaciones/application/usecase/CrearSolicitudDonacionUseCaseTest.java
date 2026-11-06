package edu.dosw.styleradar.donaciones.application.usecase;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.donaciones.domain.event.SolicitudDonacionCreadaEvent;
import edu.dosw.styleradar.donaciones.domain.exception.ConflictoDominioException;
import edu.dosw.styleradar.donaciones.domain.exception.PublicacionInvalidaException;
import edu.dosw.styleradar.donaciones.domain.exception.RecursoNoEncontradoException;
import edu.dosw.styleradar.donaciones.domain.exception.SolicitudDonacionInvalidaException;
import edu.dosw.styleradar.donaciones.domain.model.EstadoPublicacion;
import edu.dosw.styleradar.donaciones.domain.model.EstadoSolicitudDonacion;
import edu.dosw.styleradar.donaciones.domain.model.PublicacionSegundaMano;
import edu.dosw.styleradar.donaciones.domain.model.SolicitudDonacion;
import edu.dosw.styleradar.donaciones.domain.port.in.CrearSolicitudDonacionCommand;
import edu.dosw.styleradar.donaciones.domain.port.out.DonacionEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicacionRepositoryPort;
import edu.dosw.styleradar.donaciones.domain.port.out.SolicitudDonacionRepositoryPort;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - CrearSolicitudDonacionUseCaseImpl")
class CrearSolicitudDonacionUseCaseTest {

    @Mock
    private SolicitudDonacionRepositoryPort solicitudRepositoryPort;

    @Mock
    private PublicacionRepositoryPort publicacionRepositoryPort;

    @Mock
    private DonacionEventPublisherPort eventPublisherPort;

    @InjectMocks
    private CrearSolicitudDonacionUseCaseImpl crearSolicitudDonacionUseCase;

    private PublicacionSegundaMano publicacionEnDonacion;

    @BeforeEach
    void setUp() {
        publicacionEnDonacion = PublicacionSegundaMano.builder()
                .id(100L)
                .usuarioId(5L)
                .estado(EstadoPublicacion.EN_DONACION)
                .build();
    }

    @Test
    @DisplayName("Debe crear solicitud de donación exitosamente y publicar evento")
    void debeCrearSolicitudExitosamente() {
        CrearSolicitudDonacionCommand command = new CrearSolicitudDonacionCommand(
                5L, 100L, 20L, "Deseo donar esta prenda a su fundación"
        );

        when(publicacionRepositoryPort.buscarPorId(100L)).thenReturn(Optional.of(publicacionEnDonacion));
        when(solicitudRepositoryPort.existeSolicitudEnCursoParaPublicacion(100L)).thenReturn(false);
        when(solicitudRepositoryPort.guardar(any(SolicitudDonacion.class))).thenAnswer(inv -> {
            SolicitudDonacion s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        SolicitudDonacion resultado = crearSolicitudDonacionUseCase.crear(command);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals(EstadoSolicitudDonacion.PENDIENTE, resultado.getEstado());

        verify(solicitudRepositoryPort).guardar(any());
        verify(eventPublisherPort).publicarEvento(any(SolicitudDonacionCreadaEvent.class));
    }

    @Test
    @DisplayName("Debe fallar si la publicación no existe")
    void debeFallarSiPublicacionNoExiste() {
        CrearSolicitudDonacionCommand command = new CrearSolicitudDonacionCommand(5L, 999L, 20L, "Mensaje");
        when(publicacionRepositoryPort.buscarPorId(999L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> crearSolicitudDonacionUseCase.crear(command));
    }

    @Test
    @DisplayName("Debe fallar si el donante no es el dueño de la publicación")
    void debeFallarSiDonanteNoEsPropietario() {
        CrearSolicitudDonacionCommand command = new CrearSolicitudDonacionCommand(999L, 100L, 20L, "Mensaje");
        when(publicacionRepositoryPort.buscarPorId(100L)).thenReturn(Optional.of(publicacionEnDonacion));

        assertThrows(PublicacionInvalidaException.class, () -> crearSolicitudDonacionUseCase.crear(command));
    }

    @Test
    @DisplayName("Debe fallar si la publicación no está marcada en EN_DONACION")
    void debeFallarSiPublicacionNoEstaEnDonacion() {
        publicacionEnDonacion.setEstado(EstadoPublicacion.DISPONIBLE);
        CrearSolicitudDonacionCommand command = new CrearSolicitudDonacionCommand(5L, 100L, 20L, "Mensaje");
        when(publicacionRepositoryPort.buscarPorId(100L)).thenReturn(Optional.of(publicacionEnDonacion));

        assertThrows(SolicitudDonacionInvalidaException.class, () -> crearSolicitudDonacionUseCase.crear(command));
    }

    @Test
    @DisplayName("Debe fallar si ya existe una solicitud en curso para esa publicación")
    void debeFallarSiExisteSolicitudEnCurso() {
        CrearSolicitudDonacionCommand command = new CrearSolicitudDonacionCommand(5L, 100L, 20L, "Mensaje");
        when(publicacionRepositoryPort.buscarPorId(100L)).thenReturn(Optional.of(publicacionEnDonacion));
        when(solicitudRepositoryPort.existeSolicitudEnCursoParaPublicacion(100L)).thenReturn(true);

        assertThrows(ConflictoDominioException.class, () -> crearSolicitudDonacionUseCase.crear(command));
    }

    @Test
    @DisplayName("Debe consultar solicitudes de donante y de fundación")
    void debeConsultarSolicitudes() {
        when(solicitudRepositoryPort.buscarPorDonanteId(5L)).thenReturn(List.of(new SolicitudDonacion()));
        when(solicitudRepositoryPort.buscarPorFundacionId(20L)).thenReturn(List.of(new SolicitudDonacion()));
        when(solicitudRepositoryPort.buscarPorFundacionIdYEstado(20L, EstadoSolicitudDonacion.PENDIENTE))
                .thenReturn(List.of(new SolicitudDonacion()));

        assertEquals(1, crearSolicitudDonacionUseCase.consultarPorDonante(5L).size());
        assertEquals(1, crearSolicitudDonacionUseCase.consultarPorFundacion(20L, null).size());
        assertEquals(1, crearSolicitudDonacionUseCase.consultarPorFundacion(20L, EstadoSolicitudDonacion.PENDIENTE).size());
    }

    @Test
    @DisplayName("Debe obtener solicitud por ID o lanzar excepción")
    void debeObtenerPorId() {
        SolicitudDonacion sol = SolicitudDonacion.builder().id(1L).build();
        when(solicitudRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(sol));
        when(solicitudRepositoryPort.buscarPorId(999L)).thenReturn(Optional.empty());

        assertNotNull(crearSolicitudDonacionUseCase.obtenerPorId(1L));
        assertThrows(RecursoNoEncontradoException.class, () -> crearSolicitudDonacionUseCase.obtenerPorId(999L));
    }
}
