package edu.dosw.styleradar.donaciones.domain.exception;

/**
 * Excepción lanzada cuando los datos de una publicación de segunda mano no cumplen las invariantes de negocio.
 */
public class PublicacionInvalidaException extends DomainException {

    public PublicacionInvalidaException(String message) {
        super(message);
    }
}
