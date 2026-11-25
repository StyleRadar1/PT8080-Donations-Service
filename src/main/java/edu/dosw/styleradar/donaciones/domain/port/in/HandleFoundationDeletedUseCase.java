package edu.dosw.styleradar.donaciones.domain.port.in;

/**
 * Puerto de entrada para procesar la eliminación de una fundación en ms-tiendas
 * cerrando sus solicitudes de donación activas y liberando las prendas asociadas.
 */
public interface HandleFoundationDeletedUseCase {

    /**
     * Cancela las solicitudes activas asociadas a la fundación eliminada y devuelve
     * las publicaciones en estado {@code IN_DONATION} a {@code AVAILABLE}.
     *
     * @param foundationId identificador de la fundación eliminada
     * @return cantidad de solicitudes activas cerradas
     */
    int handleFoundationDeleted(Long foundationId);
}
