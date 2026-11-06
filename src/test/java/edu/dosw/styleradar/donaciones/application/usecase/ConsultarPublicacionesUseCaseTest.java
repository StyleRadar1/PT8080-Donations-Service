package edu.dosw.styleradar.donaciones.application.usecase;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.donaciones.domain.exception.RecursoNoEncontradoException;
import edu.dosw.styleradar.donaciones.domain.model.BusquedaPublicacionesCriteria;
import edu.dosw.styleradar.donaciones.domain.model.EstadoConservacion;
import edu.dosw.styleradar.donaciones.domain.model.EstadoPublicacion;
import edu.dosw.styleradar.donaciones.domain.model.Prenda;
import edu.dosw.styleradar.donaciones.domain.model.PublicacionSegundaMano;
import edu.dosw.styleradar.donaciones.domain.model.Talla;
import edu.dosw.styleradar.donaciones.domain.model.TipoPrenda;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicacionRepositoryPort;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - ConsultarPublicacionesUseCaseImpl")
class ConsultarPublicacionesUseCaseTest {

    @Mock
    private PublicacionRepositoryPort publicacionRepositoryPort;

    @InjectMocks
    private ConsultarPublicacionesUseCaseImpl consultarPublicacionesUseCase;

    @Test
    @DisplayName("Debe retornar publicación cuando existe por ID")
    void debeRetornarPublicacionPorId() {
        PublicacionSegundaMano pub = PublicacionSegundaMano.builder()
                .id(1L)
                .usuarioId(5L)
                .prenda(Prenda.builder().nombre("Camisa").tipo(TipoPrenda.SUPERIOR).build())
                .talla(Talla.M)
                .estadoConservacion(EstadoConservacion.BUEN_ESTADO)
                .precio(25000.0)
                .estado(EstadoPublicacion.DISPONIBLE)
                .build();

        when(publicacionRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(pub));

        PublicacionSegundaMano resultado = consultarPublicacionesUseCase.obtenerPorId(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("Camisa", resultado.getPrenda().getNombre());
    }

    @Test
    @DisplayName("Debe lanzar RecursoNoEncontradoException cuando no existe la publicación")
    void debeLanzarExcepcionSiNoExiste() {
        when(publicacionRepositoryPort.buscarPorId(999L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> consultarPublicacionesUseCase.obtenerPorId(999L));
    }

    @Test
    @DisplayName("Debe retornar lista de publicaciones por usuario")
    void debeRetornarPublicacionesPorUsuario() {
        PublicacionSegundaMano pub1 = PublicacionSegundaMano.builder().id(1L).usuarioId(5L).build();
        PublicacionSegundaMano pub2 = PublicacionSegundaMano.builder().id(2L).usuarioId(5L).build();

        when(publicacionRepositoryPort.buscarPorUsuarioId(5L)).thenReturn(List.of(pub1, pub2));

        List<PublicacionSegundaMano> resultado = consultarPublicacionesUseCase.obtenerPorUsuario(5L);

        assertEquals(2, resultado.size());
        verify(publicacionRepositoryPort, times(1)).buscarPorUsuarioId(5L);
    }

    @Test
    @DisplayName("Debe retornar publicaciones filtradas por criterios de búsqueda")
    void debeBuscarPorCriterios() {
        BusquedaPublicacionesCriteria criteria = BusquedaPublicacionesCriteria.builder()
                .tipo(TipoPrenda.SUPERIOR)
                .talla(Talla.M)
                .build();

        PublicacionSegundaMano pub = PublicacionSegundaMano.builder().id(1L).build();
        when(publicacionRepositoryPort.buscarConCriterios(criteria)).thenReturn(List.of(pub));

        List<PublicacionSegundaMano> resultado = consultarPublicacionesUseCase.buscar(criteria);

        assertEquals(1, resultado.size());
        verify(publicacionRepositoryPort, times(1)).buscarConCriterios(criteria);
    }
}
