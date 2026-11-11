package edu.dosw.styleradar.donaciones.domain.exception;

/**
 * Excepción lanzada al intentar realizar una transición de estado ilegal en una entidad o agregado.
 */
public class InvalidStateTransitionException extends DomainException {

    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
