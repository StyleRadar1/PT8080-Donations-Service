package edu.dosw.styleradar.donaciones.domain.exception;

/**
 * Excepción lanzada cuando dos fundaciones intentan reclamar concurrentemente la misma donación
 * o cuando la prenda ya no está disponible en la vitrina pública.
 * Mapeada a HTTP 409 Conflict.
 */
public class ConcurrentDonationClaimException extends RuntimeException {

    public ConcurrentDonationClaimException(String message) {
        super(message);
    }

    public ConcurrentDonationClaimException(String message, Throwable cause) {
        super(message, cause);
    }
}
