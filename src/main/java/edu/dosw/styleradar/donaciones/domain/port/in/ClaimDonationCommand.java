package edu.dosw.styleradar.donaciones.domain.port.in;

/**
 * Comando para que una fundación benéfica reclame una donación disponible en la Vitrina Abierta.
 */
public record ClaimDonationCommand(
        Long publicationId,
        Long foundationId,
        String message
) {
}
