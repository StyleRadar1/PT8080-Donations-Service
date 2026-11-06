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

import edu.dosw.styleradar.donaciones.domain.event.PublicacionVendidaEvent;
import edu.dosw.styleradar.donaciones.domain.exception.PublicacionInvalidaException;
import edu.dosw.styleradar.donaciones.domain.exception.RecursoNoEncontradoException;
import edu.dosw.styleradar.donaciones.domain.model.EstadoConservacion;
import edu.dosw.styleradar.donaciones.domain.model.EstadoPublicacion;
import edu.dosw.styleradar.donaciones.domain.model.Prenda;
import edu.dosw.styleradar.donaciones.domain.model.PublicacionSegundaMano;
import edu.dosw.styleradar.donaciones.domain.model.Talla;
import edu.dosw.styleradar.donaciones.domain.model.TipoPrenda;
import edu.dosw.styleradar.donaciones.domain.port.in.EditarPublicacionCommand;
import edu.dosw.styleradar.donaciones.domain.port.out.DonacionEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicacionRepositoryPort;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - GestionarPublicacionUseCaseImpl")
class GestionarPublicacionUseCaseTest {

    @Mock
    private PublicacionRepositoryPort publicacionRepositoryPort;

    @Mock
    private DonacionEventPublisherPort eventPublisherPort;

    @InjectMocks
    private GestionarPublicacionUseCaseImpl gestionarPublicacionUseCase;

    private PublicacionSegundaMano publicacion;
    private Prenda prenda;

    @BeforeEach
    void setUp() {
        prenda = Prenda.builder()
                .id(1L)
                .nombre("Vestido Floreado")
                .tipo(TipoPrenda.VESTIDO)
                .build();

        publicacion = PublicacionSegundaMano.builder()
                .id(50L)
                .usuarioId(10L)
                .prenda(prenda)
                .talla(Talla.S)
                .estadoConservacion(EstadoConservacion.COMO_NUEVO)
                .precio(80000.0)
                .estado(EstadoPublicacion.DISPONIBLE)
                .build();
    }

    @Test
    @DisplayName("Debe editar publicación exitosamente")
    void debeEditarPublicacionExitosamente() {
        EditarPublicacionCommand command = new EditarPublicacionCommand(
                10L,
                50L,
                prenda,
                Talla.M,
                EstadoConservacion.BUEN_ESTADO,
                75000.0,
                List.of("nueva_foto.jpg")
        );

        when(publicacionRepositoryPort.buscarPorId(50L)).thenReturn(Optional.of(publicacion));
        when(publicacionRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        PublicacionSegundaMano resultado = gestionarPublicacionUseCase.editar(command);

        assertEquals(Talla.M, resultado.getTalla());
        assertEquals(75000.0, resultado.getPrecio());
        verify(publicacionRepositoryPort).guardar(any());
    }

    @Test
    @DisplayName("Debe rechazar edición si el usuario no es el propietario")
    void debeRechazarEdicionSiNoEsPropietario() {
        EditarPublicacionCommand command = new EditarPublicacionCommand(
                999L, // usuario distinto
                50L,
                prenda,
                Talla.M,
                EstadoConservacion.BUEN_ESTADO,
                75000.0,
                List.of()
        );

        when(publicacionRepositoryPort.buscarPorId(50L)).thenReturn(Optional.of(publicacion));

        assertThrows(PublicacionInvalidaException.class, () -> gestionarPublicacionUseCase.editar(command));
        verify(publicacionRepositoryPort, never()).guardar(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si la publicación a editar no existe")
    void debeFallarSiPublicacionNoExiste() {
        EditarPublicacionCommand command = new EditarPublicacionCommand(
                10L, 999L, prenda, Talla.M, EstadoConservacion.BUEN_ESTADO, 75000.0, List.of()
        );

        when(publicacionRepositoryPort.buscarPorId(999L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> gestionarPublicacionUseCase.editar(command));
    }

    @Test
    @DisplayName("Debe marcar como vendida y publicar evento")
    void debeMarcarComoVendida() {
        when(publicacionRepositoryPort.buscarPorId(50L)).thenReturn(Optional.of(publicacion));
        when(publicacionRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        PublicacionSegundaMano resultado = gestionarPublicacionUseCase.marcarComoVendida(10L, 50L);

        assertEquals(EstadoPublicacion.VENDIDA, resultado.getEstado());
        verify(eventPublisherPort).publicarEvento(any(PublicacionVendidaEvent.class));
    }

    @Test
    @DisplayName("Debe retirar publicación correctamente")
    void debeRetirarPublicacion() {
        when(publicacionRepositoryPort.buscarPorId(50L)).thenReturn(Optional.of(publicacion));
        when(publicacionRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        gestionarPublicacionUseCase.retirar(10L, 50L);

        assertEquals(EstadoPublicacion.RETIRADA, publicacion.getEstado());
        verify(publicacionRepositoryPort).guardar(publicacion);
    }

    @Test
    @DisplayName("Debe destinar publicación a donación correctamente")
    void debeDestinarADonacion() {
        when(publicacionRepositoryPort.buscarPorId(50L)).thenReturn(Optional.of(publicacion));
        when(publicacionRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        PublicacionSegundaMano resultado = gestionarPublicacionUseCase.destinarADonacion(10L, 50L);

        assertEquals(EstadoPublicacion.EN_DONACION, resultado.getEstado());
        verify(publicacionRepositoryPort).guardar(publicacion);
    }
}
