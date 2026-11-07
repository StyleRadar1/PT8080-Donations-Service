package edu.dosw.styleradar.donaciones.domain.exception;

/**
 * Excepción lanzada cuando una solicitud de donación o logística incumple las condiciones requeridas.
 */
public class InvalidDonationRequestException extends DomainException {

    public InvalidDonationRequestException(String message) {
        super(message);
    }
}
