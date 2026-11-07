package edu.dosw.styleradar.donaciones.domain.exception;

/**
 * Excepción lanzada cuando una operación entra en conflicto con el estado concurrente del sistema.
 */
public class DomainConflictException extends DomainException {

    public DomainConflictException(String message) {
        super(message);
    }
}
