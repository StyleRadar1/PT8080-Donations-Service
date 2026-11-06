package edu.dosw.styleradar.donaciones.domain.model;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.dosw.styleradar.donaciones.domain.exception.PublicacionInvalidaException;
import edu.dosw.styleradar.donaciones.domain.exception.TransicionEstadoInvalidaException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Agregado PublicacionSegundaMano")
class PublicacionSegundaManoTest {

    private Prenda prendaValida;

    @BeforeEach
    void setUp() {
        prendaValida = Prenda.builder()
                .id(10L)
                .nombre("Camiseta Oversize")
                .tipo(TipoPrenda.SUPERIOR)
                .marca("Zara")
                .color("Negro")
                .estilo(Estilo.URBANO)
                .build();
    }

    @Test
    @DisplayName("Debe publicar exitosamente con invariantes válidas")
    void debePublicarExitosamente() {
        PublicacionSegundaMano pub = PublicacionSegundaMano.builder()
                .usuarioId(1L)
                .prenda(prendaValida)
                .talla(Talla.M)
                .estadoConservacion(EstadoConservacion.COMO_NUEVO)
                .precio(45000.0)
                .fotos(List.of("https://cdn.style.com/foto1.jpg", "https://cdn.style.com/foto2.jpg"))
                .build();

        pub.publicar();

        assertEquals(EstadoPublicacion.DISPONIBLE, pub.getEstado());
        assertNotNull(pub.getFechaPublicacion());
        assertNotNull(pub.getFechaActualizacion());
        assertEquals(2, pub.getFotos().size());
    }

    @Test
    @DisplayName("Debe rechazar publicación con más de 4 fotografías")
    void debeFallarSiExcedeMaximoFotos() {
        PublicacionSegundaMano pub = PublicacionSegundaMano.builder()
                .usuarioId(1L)
                .prenda(prendaValida)
                .talla(Talla.S)
                .estadoConservacion(EstadoConservacion.BUEN_ESTADO)
                .precio(30000.0)
                .fotos(List.of("f1.jpg", "f2.jpg", "f3.jpg", "f4.jpg", "f5.jpg"))
                .build();

        assertThrows(PublicacionInvalidaException.class, pub::publicar);
    }

    @Test
    @DisplayName("Debe rechazar publicación con precio negativo o nulo")
    void debeFallarSiPrecioEsInvalido() {
        PublicacionSegundaMano pub = PublicacionSegundaMano.builder()
                .usuarioId(1L)
                .prenda(prendaValida)
                .talla(Talla.L)
                .estadoConservacion(EstadoConservacion.ACEPTABLE)
                .precio(-100.0)
                .build();

        assertThrows(PublicacionInvalidaException.class, pub::publicar);

        pub.setPrecio(null);
        assertThrows(PublicacionInvalidaException.class, pub::publicar);
    }

    @Test
    @DisplayName("Debe rechazar publicación si usuarioId, prenda, talla o conservacion son nulos")
    void debeFallarPorCamposObligatoriosFaltantes() {
        PublicacionSegundaMano pub = PublicacionSegundaMano.builder().build();
        assertThrows(PublicacionInvalidaException.class, pub::publicar);

        pub.setUsuarioId(1L);
        assertThrows(PublicacionInvalidaException.class, pub::publicar);

        pub.setPrenda(prendaValida);
        assertThrows(PublicacionInvalidaException.class, pub::publicar);

        pub.setTalla(Talla.M);
        assertThrows(PublicacionInvalidaException.class, pub::publicar);

        pub.setEstadoConservacion(EstadoConservacion.BUEN_ESTADO);
        assertThrows(PublicacionInvalidaException.class, pub::publicar);
    }

    @Test
    @DisplayName("Debe editar publicación disponible correctamente")
    void debeEditarPublicacionDisponible() {
        PublicacionSegundaMano pub = PublicacionSegundaMano.builder()
                .usuarioId(1L)
                .prenda(prendaValida)
                .talla(Talla.M)
                .estadoConservacion(EstadoConservacion.COMO_NUEVO)
                .precio(50000.0)
                .build();
        pub.publicar();

        Prenda prendaActualizada = Prenda.builder()
                .id(10L)
                .nombre("Camiseta Slim")
                .tipo(TipoPrenda.SUPERIOR)
                .build();

        pub.editar(prendaActualizada, Talla.S, EstadoConservacion.BUEN_ESTADO, 40000.0, List.of("foto_nueva.jpg"));

        assertEquals("Camiseta Slim", pub.getPrenda().getNombre());
        assertEquals(Talla.S, pub.getTalla());
        assertEquals(40000.0, pub.getPrecio());
        assertEquals(1, pub.getFotos().size());
    }

    @Test
    @DisplayName("Debe impedir editar publicación si está vendida, retirada o donada")
    void debeImpedirEditarEnEstadosBloqueantes() {
        PublicacionSegundaMano pub = PublicacionSegundaMano.builder()
                .usuarioId(1L)
                .prenda(prendaValida)
                .talla(Talla.M)
                .estadoConservacion(EstadoConservacion.COMO_NUEVO)
                .precio(50000.0)
                .build();
        pub.publicar();

        pub.setEstado(EstadoPublicacion.VENDIDA);
        assertThrows(TransicionEstadoInvalidaException.class,
                () -> pub.editar(prendaValida, Talla.M, EstadoConservacion.COMO_NUEVO, 30000.0, null));

        pub.setEstado(EstadoPublicacion.RETIRADA);
        assertThrows(TransicionEstadoInvalidaException.class,
                () -> pub.editar(prendaValida, Talla.M, EstadoConservacion.COMO_NUEVO, 30000.0, null));

        pub.setEstado(EstadoPublicacion.DONADA);
        assertThrows(TransicionEstadoInvalidaException.class,
                () -> pub.editar(prendaValida, Talla.M, EstadoConservacion.COMO_NUEVO, 30000.0, null));
    }

    @Test
    @DisplayName("Debe marcar como vendida y validar transiciones ilegales")
    void debeGestionarMarcadoDeVenta() {
        PublicacionSegundaMano pub = PublicacionSegundaMano.builder()
                .usuarioId(1L)
                .prenda(prendaValida)
                .talla(Talla.M)
                .estadoConservacion(EstadoConservacion.COMO_NUEVO)
                .precio(50000.0)
                .build();
        pub.publicar();

        pub.marcarComoVendida();
        assertEquals(EstadoPublicacion.VENDIDA, pub.getEstado());

        assertThrows(TransicionEstadoInvalidaException.class, pub::marcarComoVendida);

        pub.setEstado(EstadoPublicacion.RETIRADA);
        assertThrows(TransicionEstadoInvalidaException.class, pub::marcarComoVendida);

        pub.setEstado(EstadoPublicacion.EN_DONACION);
        assertThrows(TransicionEstadoInvalidaException.class, pub::marcarComoVendida);

        pub.setEstado(EstadoPublicacion.DONADA);
        assertThrows(TransicionEstadoInvalidaException.class, pub::marcarComoVendida);
    }

    @Test
    @DisplayName("Debe retirar publicación y validar transiciones ilegales")
    void debeGestionarRetiro() {
        PublicacionSegundaMano pub = PublicacionSegundaMano.builder()
                .usuarioId(1L)
                .prenda(prendaValida)
                .talla(Talla.M)
                .estadoConservacion(EstadoConservacion.COMO_NUEVO)
                .precio(50000.0)
                .build();
        pub.publicar();

        pub.retirar();
        assertEquals(EstadoPublicacion.RETIRADA, pub.getEstado());
        assertThrows(TransicionEstadoInvalidaException.class, pub::retirar);

        pub.setEstado(EstadoPublicacion.DONADA);
        assertThrows(TransicionEstadoInvalidaException.class, pub::retirar);
    }

    @Test
    @DisplayName("Debe gestionar ciclo hacia donación y donada correctamente")
    void debeGestionarFlujoDonacion() {
        PublicacionSegundaMano pub = PublicacionSegundaMano.builder()
                .usuarioId(1L)
                .prenda(prendaValida)
                .talla(Talla.M)
                .estadoConservacion(EstadoConservacion.BUEN_ESTADO)
                .precio(0.0)
                .build();
        pub.publicar();

        pub.destinarADonacion();
        assertEquals(EstadoPublicacion.EN_DONACION, pub.getEstado());

        // No puede destinarse a donacion si ya no esta DISPONIBLE
        assertThrows(TransicionEstadoInvalidaException.class, pub::destinarADonacion);

        pub.marcarComoDonada();
        assertEquals(EstadoPublicacion.DONADA, pub.getEstado());

        // No puede marcarse como donada si ya no esta EN_DONACION
        assertThrows(TransicionEstadoInvalidaException.class, pub::marcarComoDonada);
    }
}
