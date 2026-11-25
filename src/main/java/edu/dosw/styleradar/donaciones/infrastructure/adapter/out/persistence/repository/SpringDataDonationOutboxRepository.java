package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.dosw.styleradar.donaciones.domain.model.OutboxEventStatus;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.entity.DonationOutboxEventEntity;

/**
 * Repositorio Spring Data JPA para el Outbox transaccional de donaciones.
 */
@Repository
public interface SpringDataDonationOutboxRepository extends JpaRepository<DonationOutboxEventEntity, Long> {

    boolean existsByRequestIdAndEventType(Long requestId, String eventType);

    List<DonationOutboxEventEntity> findByStatusOrderByCreatedAtAscIdAsc(OutboxEventStatus status);

    List<DonationOutboxEventEntity> findByFoundationIdAndStatusOrderByCreatedAtAscIdAsc(
            Long foundationId, OutboxEventStatus status);
}
