package edu.dosw.styleradar.donaciones.domain.exception;

/**
 * Excepción lanzada cuando una solicitud de donación o logística incumple las condiciones requeridas.
 */
public class SolicitudDonacionInvalidaException extends DomainException {

    public SolicitudDonacionInvalidaException(String message) {
        super(message);
    }
}
