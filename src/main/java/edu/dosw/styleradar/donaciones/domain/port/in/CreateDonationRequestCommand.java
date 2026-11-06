package edu.dosw.styleradar.donaciones.domain.port.in;

/**
 * Comando para radicar una solicitud de donación sobre una prenda publicada hacia una fundación.
 */
public record CreateDonationRequestCommand(
        Long donorId,
        Long publicationId,
        Long foundationId,
        String message
) {
}
