package edu.dosw.styleradar.donaciones.domain.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import edu.dosw.styleradar.donaciones.domain.model.DonationOutboxEvent;

/**
 * Puerto de salida para la persistencia y reclamación atómica del Outbox transaccional.
 */
public interface DonationOutboxRepositoryPort {

    DonationOutboxEvent save(DonationOutboxEvent event);

    boolean existsByRequestIdAndEventType(Long requestId, String eventType);

    List<DonationOutboxEvent> findDispatchableEvents(LocalDateTime staleBefore);

    List<DonationOutboxEvent> findDispatchableByFoundationId(Long foundationId, LocalDateTime staleBefore);

    Optional<DonationOutboxEvent> claimForDispatch(
            Long id, String claimToken, LocalDateTime now, LocalDateTime staleBefore);

    boolean markSent(Long id, String claimToken, LocalDateTime sentAt);

    void releaseForRetry(Long id, String claimToken, String errorMessage);
}
