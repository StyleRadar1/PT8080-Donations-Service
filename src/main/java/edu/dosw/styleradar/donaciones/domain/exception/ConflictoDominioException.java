package edu.dosw.styleradar.donaciones.domain.exception;

/**
 * Excepción lanzada cuando una operación entra en conflicto con el estado concurrente o actual del sistema.
 */
public class ConflictoDominioException extends DomainException {

    public ConflictoDominioException(String message) {
        super(message);
    }
}
