package edu.dosw.styleradar.donaciones.domain.exception;

/**
 * Excepción base para el dominio puro. Representa cualquier violación de reglas de negocio e invariantes.
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
