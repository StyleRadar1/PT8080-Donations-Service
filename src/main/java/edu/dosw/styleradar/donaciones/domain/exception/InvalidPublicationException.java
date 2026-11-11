package edu.dosw.styleradar.donaciones.domain.exception;

/**
 * Excepción lanzada cuando los datos de una publicación no cumplen las invariantes de negocio.
 */
public class InvalidPublicationException extends DomainException {

    public InvalidPublicationException(String message) {
        super(message);
    }
}
