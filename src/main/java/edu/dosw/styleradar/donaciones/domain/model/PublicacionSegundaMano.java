package edu.dosw.styleradar.donaciones.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import edu.dosw.styleradar.donaciones.domain.exception.PublicacionInvalidaException;
import edu.dosw.styleradar.donaciones.domain.exception.TransicionEstadoInvalidaException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agregado raíz que representa una publicación de prenda usada en el módulo de Economía Circular.
 * Encapsula la lógica del ciclo de vida (publicar, editar, vender, retirar, donar) y sus invariantes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicacionSegundaMano {

    public static final int MAX_FOTOS = 4;

    private Long id;
    private Long usuarioId;
    private Prenda prenda;
    private Talla talla;
    private EstadoConservacion estadoConservacion;
    private Double precio;
    private EstadoPublicacion estado;
    private Instant fechaPublicacion;
    private Instant fechaActualizacion;

    @Builder.Default
    private List<String> fotos = new ArrayList<>();

    /**
     * Valida las invariantes fundamentales de la publicación.
     */
    public void validarInvariantes() {
        if (usuarioId == null) {
            throw new PublicacionInvalidaException("El ID del usuario propietario es obligatorio");
        }
        if (prenda == null) {
            throw new PublicacionInvalidaException("La prenda asociada es obligatoria");
        }
        prenda.validarInvariantes();

        if (talla == null) {
            throw new PublicacionInvalidaException("La talla de la prenda es obligatoria");
        }
        if (estadoConservacion == null) {
            throw new PublicacionInvalidaException("El estado de conservación es obligatorio");
        }
        if (precio == null || precio < 0) {
            throw new PublicacionInvalidaException("El precio de la prenda usada debe ser mayor o igual a 0");
        }
        if (fotos != null && fotos.size() > MAX_FOTOS) {
            throw new PublicacionInvalidaException("Una publicación no puede contener más de " + MAX_FOTOS + " fotografías");
        }
    }

    /**
     * Inicializa la publicación como activa y disponible en el catálogo de reventa.
     */
    public void publicar() {
        validarInvariantes();
        this.estado = EstadoPublicacion.DISPONIBLE;
        Instant ahora = Instant.now();
        this.fechaPublicacion = ahora;
        this.fechaActualizacion = ahora;
    }

    /**
     * Modifica los datos de la publicación verificando que no se encuentre en un estado bloqueante.
     */
    public void editar(Prenda nuevaPrenda, Talla nuevaTalla, EstadoConservacion nuevoEstadoConservacion,
                       Double nuevoPrecio, List<String> nuevasFotos) {
        if (estado == EstadoPublicacion.VENDIDA) {
            throw new TransicionEstadoInvalidaException("No se puede editar una publicación que ya ha sido vendida");
        }
        if (estado == EstadoPublicacion.RETIRADA) {
            throw new TransicionEstadoInvalidaException("No se puede editar una publicación que ha sido retirada");
        }
        if (estado == EstadoPublicacion.DONADA) {
            throw new TransicionEstadoInvalidaException("No se puede editar una prenda que ya fue donada");
        }

        this.prenda = nuevaPrenda;
        this.talla = nuevaTalla;
        this.estadoConservacion = nuevoEstadoConservacion;
        this.precio = nuevoPrecio;
        this.fotos = nuevasFotos != null ? new ArrayList<>(nuevasFotos) : new ArrayList<>();

        validarInvariantes();
        this.fechaActualizacion = Instant.now();
    }

    /**
     * Marca la publicación como vendida.
     */
    public void marcarComoVendida() {
        if (estado == EstadoPublicacion.VENDIDA) {
            throw new TransicionEstadoInvalidaException("La publicación ya se encuentra marcada como vendida");
        }
        if (estado == EstadoPublicacion.RETIRADA) {
            throw new TransicionEstadoInvalidaException("No se puede marcar como vendida una publicación retirada");
        }
        if (estado == EstadoPublicacion.EN_DONACION || estado == EstadoPublicacion.DONADA) {
            throw new TransicionEstadoInvalidaException("No se puede vender una prenda destinada a donación");
        }

        this.estado = EstadoPublicacion.VENDIDA;
        this.fechaActualizacion = Instant.now();
    }

    /**
     * Retira la publicación del catálogo público.
     */
    public void retirar() {
        if (estado == EstadoPublicacion.RETIRADA) {
            throw new TransicionEstadoInvalidaException("La publicación ya se encuentra retirada");
        }
        if (estado == EstadoPublicacion.DONADA) {
            throw new TransicionEstadoInvalidaException("No se puede retirar una prenda que ya fue donada");
        }

        this.estado = EstadoPublicacion.RETIRADA;
        this.fechaActualizacion = Instant.now();
    }

    /**
     * Destina la prenda hacia el canal benéfico de donaciones a fundaciones.
     */
    public void destinarADonacion() {
        if (estado != EstadoPublicacion.DISPONIBLE) {
            throw new TransicionEstadoInvalidaException("Solo una publicación disponible puede destinarse a donación");
        }

        this.estado = EstadoPublicacion.EN_DONACION;
        this.fechaActualizacion = Instant.now();
    }

    /**
     * Concluye el ciclo benéfico marcando la prenda como donada a la fundación.
     */
    public void marcarComoDonada() {
        if (estado != EstadoPublicacion.EN_DONACION) {
            throw new TransicionEstadoInvalidaException("Solo una prenda marcada en donación puede pasar a estado donada");
        }

        this.estado = EstadoPublicacion.DONADA;
        this.fechaActualizacion = Instant.now();
    }
}
