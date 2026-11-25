package edu.dosw.styleradar.donaciones.domain.port.out;

import java.util.List;

import edu.dosw.styleradar.donaciones.domain.model.DonationOutboxEvent;

/**
 * Puerto de salida para la persistencia del Outbox transaccional de eventos de donación.
 */
public interface DonationOutboxRepositoryPort {

    DonationOutboxEvent save(DonationOutboxEvent event);

    boolean existsByRequestIdAndEventType(Long requestId, String eventType);

    List<DonationOutboxEvent> findPendingEvents();

    List<DonationOutboxEvent> findPendingByFoundationId(Long foundationId);
}
