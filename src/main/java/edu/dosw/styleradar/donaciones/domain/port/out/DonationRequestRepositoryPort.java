package edu.dosw.styleradar.donaciones.domain.port.out;

import java.util.List;
import java.util.Optional;

import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequestStatus;

/**
 * Puerto de salida (SPI) para la persistencia y recuperación de solicitudes de donación.
 */
public interface DonationRequestRepositoryPort {

    DonationRequest save(DonationRequest request);

    Optional<DonationRequest> findById(Long id);

    List<DonationRequest> findByDonorId(Long donorId);

    List<DonationRequest> findByFoundationId(Long foundationId);

    List<DonationRequest> findByFoundationIdAndStatus(Long foundationId, DonationRequestStatus status);

    boolean existsActiveRequestForPublication(Long publicationId);
}
