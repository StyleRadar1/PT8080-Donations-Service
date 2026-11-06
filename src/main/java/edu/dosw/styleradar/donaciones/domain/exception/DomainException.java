package edu.dosw.styleradar.donaciones.domain.exception;

/**
 * Excepción base para todas las violaciones de invariantes y reglas de negocio del dominio puro.
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
