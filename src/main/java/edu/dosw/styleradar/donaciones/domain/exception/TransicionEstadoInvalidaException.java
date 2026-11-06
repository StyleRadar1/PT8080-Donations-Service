package edu.dosw.styleradar.donaciones.domain.exception;

/**
 * Excepción lanzada cuando se intenta una transición de estado ilegal en una publicación o solicitud.
 */
public class TransicionEstadoInvalidaException extends DomainException {

    public TransicionEstadoInvalidaException(String message) {
        super(message);
    }
}
