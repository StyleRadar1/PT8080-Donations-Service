package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.donaciones.domain.model.DonationOutboxEvent;
import edu.dosw.styleradar.donaciones.domain.model.OutboxEventStatus;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationOutboxRepositoryPort;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.entity.DonationOutboxEventEntity;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.mapper.DonationOutboxPersistenceMapper;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.repository.SpringDataDonationOutboxRepository;
import lombok.RequiredArgsConstructor;

/**
 * Adaptador de salida JPA para persistir y consultar eventos pendientes en el Outbox transaccional.
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
    public List<DonationOutboxEvent> findPendingEvents() {
        return repository.findByStatusOrderByCreatedAtAscIdAsc(OutboxEventStatus.PENDING)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DonationOutboxEvent> findPendingByFoundationId(Long foundationId) {
        return repository.findByFoundationIdAndStatusOrderByCreatedAtAscIdAsc(
                        foundationId, OutboxEventStatus.PENDING)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
