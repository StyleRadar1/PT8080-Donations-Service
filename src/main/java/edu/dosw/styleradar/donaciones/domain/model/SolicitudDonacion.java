package edu.dosw.styleradar.donaciones.domain.model;

import java.time.Instant;

import edu.dosw.styleradar.donaciones.domain.exception.SolicitudDonacionInvalidaException;
import edu.dosw.styleradar.donaciones.domain.exception.TransicionEstadoInvalidaException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad de dominio que gestiona la solicitud de donación y la logística de entrega a una fundación benéfica.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudDonacion {

    private Long id;
    private Long publicacionId;
    private Long donanteId;
    private Long fundacionId;
    private String mensaje;
    private EstadoSolicitudDonacion estado;
    private String motivoRechazo;
    private String puntoEntrega;
    private Instant franjaInicio;
    private Instant franjaFin;
    private Instant fechaSolicitud;
    private Instant fechaDecision;
    private Instant fechaRecepcion;

    /**
     * Valida las invariantes requeridas al originar una nueva solicitud de donación.
     */
    public void validarInvariantesIniciales() {
        if (publicacionId == null) {
            throw new SolicitudDonacionInvalidaException("El ID de la publicación es obligatorio");
        }
        if (donanteId == null) {
            throw new SolicitudDonacionInvalidaException("El ID del donante es obligatorio");
        }
        if (fundacionId == null) {
            throw new SolicitudDonacionInvalidaException("El ID de la fundación destinataria es obligatorio");
        }
    }

    /**
     * Inicializa la solicitud en estado PENDIENTE de revisión por parte de la fundación.
     */
    public void iniciarSolicitud() {
        validarInvariantesIniciales();
        this.estado = EstadoSolicitudDonacion.PENDIENTE;
        this.fechaSolicitud = Instant.now();
    }

    /**
     * Aprueba la solicitud por parte del representante de la fundación benéfica.
     */
    public void aprobar() {
        if (estado != EstadoSolicitudDonacion.PENDIENTE) {
            throw new TransicionEstadoInvalidaException(
                    "No se puede aprobar una solicitud que no esté en estado PENDIENTE. Estado actual: " + estado);
        }
        this.estado = EstadoSolicitudDonacion.APROBADA;
        this.fechaDecision = Instant.now();
    }

    /**
     * Rechaza la solicitud especificando el motivo justificativo.
     */
    public void rechazar(String motivo) {
        if (estado != EstadoSolicitudDonacion.PENDIENTE) {
            throw new TransicionEstadoInvalidaException(
                    "No se puede rechazar una solicitud que no esté en estado PENDIENTE. Estado actual: " + estado);
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new SolicitudDonacionInvalidaException("El motivo de rechazo es obligatorio");
        }
        this.estado = EstadoSolicitudDonacion.RECHAZADA;
        this.motivoRechazo = motivo.trim();
        this.fechaDecision = Instant.now();
    }

    /**
     * Coordina y programa la cita logística de recogida o punto de entrega de las prendas.
     */
    public void programarRecogida(String punto, Instant inicio, Instant fin) {
        if (estado != EstadoSolicitudDonacion.APROBADA && estado != EstadoSolicitudDonacion.EN_RECOGIDA) {
            throw new TransicionEstadoInvalidaException(
                    "Solo se puede programar o reprogramar la recogida de una solicitud aprobada. Estado actual: " + estado);
        }
        if (punto == null || punto.trim().isEmpty()) {
            throw new SolicitudDonacionInvalidaException("El punto de entrega o recogida es obligatorio");
        }
        if (inicio == null || fin == null) {
            throw new SolicitudDonacionInvalidaException("Las fechas de la franja horaria son obligatorias");
        }
        if (!fin.isAfter(inicio)) {
            throw new SolicitudDonacionInvalidaException("El fin de la franja horaria debe ser posterior al inicio");
        }

        this.puntoEntrega = punto.trim();
        this.franjaInicio = inicio;
        this.franjaFin = fin;
        this.estado = EstadoSolicitudDonacion.EN_RECOGIDA;
    }

    /**
     * Confirma la entrega efectiva de la donación a la sede o personal de la fundación.
     */
    public void confirmarEntrega() {
        if (estado != EstadoSolicitudDonacion.EN_RECOGIDA) {
            throw new TransicionEstadoInvalidaException(
                    "Solo se puede confirmar la entrega de una solicitud en proceso de recogida. Estado actual: " + estado);
        }
        this.estado = EstadoSolicitudDonacion.ENTREGADA;
        this.fechaRecepcion = Instant.now();
    }
}
