package edu.dosw.styleradar.donaciones.domain.exception;

/**
 * Excepción lanzada cuando una entidad de dominio requerida no existe en el repositorio.
 */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
