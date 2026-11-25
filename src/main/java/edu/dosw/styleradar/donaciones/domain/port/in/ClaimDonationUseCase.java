package edu.dosw.styleradar.donaciones.domain.port.in;

import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;

/**
 * Puerto de entrada (Use Case) para el reclamo de prendas en la Vitrina Abierta de Donaciones.
 */
public interface ClaimDonationUseCase {

    /**
     * Permite a una fundación reclamar una donación benéfica disponible.
     * Concurrencia optimista: Si dos fundaciones intentan reclamar concurrentemente,
     * la perdedora recibe una excepción de conflicto (HTTP 409).
     */
    DonationRequest claimDonation(ClaimDonationCommand command);
}
