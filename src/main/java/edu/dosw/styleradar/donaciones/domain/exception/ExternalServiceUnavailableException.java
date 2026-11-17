package edu.dosw.styleradar.donaciones.domain.exception;

/**
 * Excepción lanzada cuando un microservicio externo requerido (como ms-tiendas)
 * no responde o retorna un error técnico inesperado.
 */
public class ExternalServiceUnavailableException extends DomainException {

    public ExternalServiceUnavailableException(String message) {
        super(message);
    }

    public ExternalServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
