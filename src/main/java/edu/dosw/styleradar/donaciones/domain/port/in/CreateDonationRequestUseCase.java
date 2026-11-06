package edu.dosw.styleradar.donaciones.domain.port.in;

import java.util.List;

import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequestStatus;

/**
 * Puerto de entrada (Caso de Uso): Radicación y consulta de solicitudes de donación.
 */
public interface CreateDonationRequestUseCase {

    DonationRequest create(CreateDonationRequestCommand command);

    List<DonationRequest> getByDonorId(Long donorId);

    List<DonationRequest> getByFoundationId(Long foundationId, DonationRequestStatus status);

    DonationRequest getById(Long id);
}
