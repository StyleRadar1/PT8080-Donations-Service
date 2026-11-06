package edu.dosw.styleradar.donaciones.application.usecase;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.donaciones.domain.event.PublicacionCreadaEvent;
import edu.dosw.styleradar.donaciones.domain.exception.PublicacionInvalidaException;
import edu.dosw.styleradar.donaciones.domain.model.EstadoConservacion;
import edu.dosw.styleradar.donaciones.domain.model.EstadoPublicacion;
import edu.dosw.styleradar.donaciones.domain.model.Estilo;
import edu.dosw.styleradar.donaciones.domain.model.Prenda;
import edu.dosw.styleradar.donaciones.domain.model.PublicacionSegundaMano;
import edu.dosw.styleradar.donaciones.domain.model.Talla;
import edu.dosw.styleradar.donaciones.domain.model.TipoPrenda;
import edu.dosw.styleradar.donaciones.domain.port.in.CrearPublicacionCommand;
import edu.dosw.styleradar.donaciones.domain.port.out.DonacionEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicacionRepositoryPort;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - CrearPublicacionUseCaseImpl")
class CrearPublicacionUseCaseTest {

    @Mock
    private PublicacionRepositoryPort publicacionRepositoryPort;

    @Mock
    private DonacionEventPublisherPort eventPublisherPort;

    @InjectMocks
    private CrearPublicacionUseCaseImpl crearPublicacionUseCase;

    private Prenda prenda;

    @BeforeEach
    void setUp() {
        prenda = Prenda.builder()
                .id(1L)
                .nombre("Buzo Oversize")
                .tipo(TipoPrenda.SUPERIOR)
                .marca("H&M")
                .color("Gris")
                .estilo(Estilo.URBANO)
                .build();
    }

    @Test
    @DisplayName("Debe crear la publicación exitosamente y publicar evento de dominio")
    void debeCrearPublicacionExitosamente() {
        CrearPublicacionCommand command = new CrearPublicacionCommand(
                10L,
                prenda,
                Talla.L,
                EstadoConservacion.COMO_NUEVO,
                65000.0,
                List.of("https://cdn.style.com/foto1.jpg")
        );

        when(publicacionRepositoryPort.guardar(any(PublicacionSegundaMano.class))).thenAnswer(inv -> {
            PublicacionSegundaMano p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });

        PublicacionSegundaMano resultado = crearPublicacionUseCase.crear(command);

        assertNotNull(resultado);
        assertEquals(100L, resultado.getId());
        assertEquals(EstadoPublicacion.DISPONIBLE, resultado.getEstado());
        assertEquals(65000.0, resultado.getPrecio());

        verify(publicacionRepositoryPort, times(1)).guardar(any(PublicacionSegundaMano.class));
        verify(eventPublisherPort, times(1)).publicarEvento(any(PublicacionCreadaEvent.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el comando contiene datos inválidos")
    void debeFallarSiDatosSonInvalidos() {
        CrearPublicacionCommand commandInvalido = new CrearPublicacionCommand(
                10L,
                prenda,
                Talla.L,
                EstadoConservacion.COMO_NUEVO,
                -50.0, // precio negativo
                List.of()
        );

        assertThrows(PublicacionInvalidaException.class, () -> crearPublicacionUseCase.crear(commandInvalido));

        verify(publicacionRepositoryPort, never()).guardar(any());
        verify(eventPublisherPort, never()).publicarEvento(any());
    }
}
