package edu.dosw.styleradar.donaciones.domain.port.in;

import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;

/**
 * Puerto de entrada (Caso de Uso): Confirmación de recepción física de las prendas en la fundación.
 */
public interface ConfirmDonationReceiptUseCase {

    DonationRequest confirmReceipt(Long foundationId, Long requestId);
}
