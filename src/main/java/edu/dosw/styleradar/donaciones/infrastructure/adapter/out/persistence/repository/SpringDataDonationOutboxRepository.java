package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.dosw.styleradar.donaciones.domain.model.OutboxEventStatus;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.entity.DonationOutboxEventEntity;

/**
 * Repositorio Spring Data JPA para el Outbox transaccional de donaciones con reclamación atómica.
 */
@Repository
public interface SpringDataDonationOutboxRepository extends JpaRepository<DonationOutboxEventEntity, Long> {

    boolean existsByRequestIdAndEventType(Long requestId, String eventType);

    List<DonationOutboxEventEntity> findByStatusOrderByCreatedAtAscIdAsc(OutboxEventStatus status);

    @Query("""
            SELECT e FROM DonationOutboxEventEntity e
            WHERE e.status = :pendingStatus
               OR (e.status = :processingStatus AND e.claimedAt < :staleBefore)
            ORDER BY e.createdAt ASC, e.id ASC
            """)
    List<DonationOutboxEventEntity> findDispatchableEvents(
            @Param("pendingStatus") OutboxEventStatus pendingStatus,
            @Param("processingStatus") OutboxEventStatus processingStatus,
            @Param("staleBefore") LocalDateTime staleBefore);

    @Query("""
            SELECT e FROM DonationOutboxEventEntity e
            WHERE e.foundationId = :foundationId
              AND (e.status = :pendingStatus
                   OR (e.status = :processingStatus AND e.claimedAt < :staleBefore))
            ORDER BY e.createdAt ASC, e.id ASC
            """)
    List<DonationOutboxEventEntity> findDispatchableByFoundationId(
            @Param("foundationId") Long foundationId,
            @Param("pendingStatus") OutboxEventStatus pendingStatus,
            @Param("processingStatus") OutboxEventStatus processingStatus,
            @Param("staleBefore") LocalDateTime staleBefore);

    @Query("""
            SELECT e FROM DonationOutboxEventEntity e
            WHERE e.requestId = :requestId
              AND (e.status = :pendingStatus
                   OR (e.status = :processingStatus AND e.claimedAt < :staleBefore))
            ORDER BY e.createdAt ASC, e.id ASC
            """)
    List<DonationOutboxEventEntity> findDispatchableByRequestId(
            @Param("requestId") Long requestId,
            @Param("pendingStatus") OutboxEventStatus pendingStatus,
            @Param("processingStatus") OutboxEventStatus processingStatus,
            @Param("staleBefore") LocalDateTime staleBefore);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE DonationOutboxEventEntity e
            SET e.status = :processingStatus,
                e.claimedBy = :claimToken,
                e.claimedAt = :now
            WHERE e.id = :id
              AND (e.status = :pendingStatus
                   OR (e.status = :processingStatus AND e.claimedAt < :staleBefore))
            """)
    int claimEventAtomically(
            @Param("id") Long id,
            @Param("claimToken") String claimToken,
            @Param("now") LocalDateTime now,
            @Param("staleBefore") LocalDateTime staleBefore,
            @Param("pendingStatus") OutboxEventStatus pendingStatus,
            @Param("processingStatus") OutboxEventStatus processingStatus);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE DonationOutboxEventEntity e
            SET e.status = :sentStatus,
                e.attempts = e.attempts + 1,
                e.lastError = null,
                e.sentAt = :sentAt,
                e.claimedBy = null,
                e.claimedAt = null
            WHERE e.id = :id
              AND e.status = :processingStatus
              AND e.claimedBy = :claimToken
            """)
    int markSentIfClaimed(
            @Param("id") Long id,
            @Param("claimToken") String claimToken,
            @Param("sentAt") LocalDateTime sentAt,
            @Param("processingStatus") OutboxEventStatus processingStatus,
            @Param("sentStatus") OutboxEventStatus sentStatus);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE DonationOutboxEventEntity e
            SET e.status = :pendingStatus,
                e.attempts = e.attempts + 1,
                e.lastError = :lastError,
                e.claimedBy = null,
                e.claimedAt = null
            WHERE e.id = :id
              AND e.status = :processingStatus
              AND e.claimedBy = :claimToken
            """)
    int releaseClaimForRetry(
            @Param("id") Long id,
            @Param("claimToken") String claimToken,
            @Param("lastError") String lastError,
            @Param("processingStatus") OutboxEventStatus processingStatus,
            @Param("pendingStatus") OutboxEventStatus pendingStatus);
}
