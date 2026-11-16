package edu.dosw.styleradar.donaciones.domain.port.out;

import java.util.List;
import java.util.Optional;

import edu.dosw.styleradar.donaciones.domain.model.FoundationSummary;

/**
 * Puerto de salida para consultar y validar fundaciones aliadas registradas en ms-tiendas.
 */
public interface FoundationCatalogPort {

    /**
     * Obtiene el listado completo de fundaciones aliadas activas disponibles para recibir donaciones.
     */
    List<FoundationSummary> findAllActiveFoundations();

    /**
     * Consulta la información de una fundación específica por su identificador en ms-tiendas.
     */
    Optional<FoundationSummary> findFoundationById(Long foundationId);

    /**
     * Valida que la fundación exista y se encuentre activa antes de registrar o reclamar una donación.
     */
    void validateActiveFoundation(Long foundationId);
}
