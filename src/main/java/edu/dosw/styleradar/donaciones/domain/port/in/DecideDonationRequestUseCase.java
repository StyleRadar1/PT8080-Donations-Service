package edu.dosw.styleradar.donaciones.domain.port.in;

import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;

/**
 * Puerto de entrada (Caso de Uso): Aprobación o rechazo de solicitudes de donación por fundaciones.
 */
public interface DecideDonationRequestUseCase {

    DonationRequest decide(Long foundationId, Long requestId, boolean approve, String reason);
}
