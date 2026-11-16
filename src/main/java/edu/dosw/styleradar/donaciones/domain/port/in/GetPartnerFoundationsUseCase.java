package edu.dosw.styleradar.donaciones.domain.port.in;

import java.util.List;

import edu.dosw.styleradar.donaciones.domain.model.FoundationSummary;

/**
 * Puerto de entrada para consultar las fundaciones aliadas disponibles para el selector de donaciones (SR-RF-44).
 */
public interface GetPartnerFoundationsUseCase {

    /**
     * Lista las fundaciones activas aptas para recibir donaciones, con filtro opcional por categoría de prenda.
     */
    List<FoundationSummary> getActiveFoundations(String category);

    /**
     * Obtiene el detalle de una fundación aliada activa por su identificador.
     */
    FoundationSummary getActiveFoundationById(Long foundationId);
}
