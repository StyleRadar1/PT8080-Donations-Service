package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.donaciones.domain.model.DonationOutboxEvent;
import edu.dosw.styleradar.donaciones.domain.model.OutboxEventStatus;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationOutboxRepositoryPort;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.entity.DonationOutboxEventEntity;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.mapper.DonationOutboxPersistenceMapper;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.repository.SpringDataDonationOutboxRepository;
import lombok.RequiredArgsConstructor;

/**
 * Adaptador de salida JPA para persistir, consultar y reclamar atómicamente eventos del Outbox.
 */
@Component
@RequiredArgsConstructor
public class DonationOutboxPersistenceAdapter implements DonationOutboxRepositoryPort {

    private final SpringDataDonationOutboxRepository repository;
    private final DonationOutboxPersistenceMapper mapper;

    @Override
    @Transactional
    public DonationOutboxEvent save(DonationOutboxEvent event) {
        DonationOutboxEventEntity saved = repository.save(mapper.toEntity(event));
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByRequestIdAndEventType(Long requestId, String eventType) {
        return repository.existsByRequestIdAndEventType(requestId, eventType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DonationOutboxEvent> findDispatchableEvents(LocalDateTime staleBefore) {
        return repository.findDispatchableEvents(
                        OutboxEventStatus.PENDING, OutboxEventStatus.PROCESSING, staleBefore)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DonationOutboxEvent> findDispatchableByFoundationId(Long foundationId, LocalDateTime staleBefore) {
        return repository.findDispatchableByFoundationId(
                        foundationId, OutboxEventStatus.PENDING, OutboxEventStatus.PROCESSING, staleBefore)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DonationOutboxEvent> findDispatchableByRequestId(Long requestId, LocalDateTime staleBefore) {
        return repository.findDispatchableByRequestId(
                        requestId, OutboxEventStatus.PENDING, OutboxEventStatus.PROCESSING, staleBefore)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<DonationOutboxEvent> claimForDispatch(
            Long id, String claimToken, LocalDateTime now, LocalDateTime staleBefore) {
        int updated = repository.claimEventAtomically(
                id,
                claimToken,
                now,
                staleBefore,
                OutboxEventStatus.PENDING,
                OutboxEventStatus.PROCESSING
        );
        if (updated == 0) {
            return Optional.empty();
        }
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markSent(Long id, String claimToken, LocalDateTime sentAt) {
        int updated = repository.markSentIfClaimed(
                id,
                claimToken,
                sentAt,
                OutboxEventStatus.PROCESSING,
                OutboxEventStatus.SENT
        );
        return updated > 0;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseForRetry(Long id, String claimToken, String errorMessage) {
        repository.releaseClaimForRetry(
                id,
                claimToken,
                DonationOutboxEvent.normalizeError(errorMessage),
                OutboxEventStatus.PROCESSING,
                OutboxEventStatus.PENDING
        );
    }
}
