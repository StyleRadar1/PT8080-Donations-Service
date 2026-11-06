package edu.dosw.styleradar.donaciones.domain.exception;

/**
 * Excepción lanzada cuando una entidad requerida no existe en el repositorio de dominio.
 */
public class RecursoNoEncontradoException extends DomainException {

    public RecursoNoEncontradoException(String message) {
        super(message);
    }
}
