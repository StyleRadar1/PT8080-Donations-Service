package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import edu.dosw.styleradar.donaciones.domain.model.DonationOutboxEvent;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.entity.DonationOutboxEventEntity;

/**
 * Mapper entre la entidad JPA {@link DonationOutboxEventEntity} y el modelo de dominio {@link DonationOutboxEvent}.
 */
@Component
public class DonationOutboxPersistenceMapper {

    public DonationOutboxEventEntity toEntity(DonationOutboxEvent domain) {
        if (domain == null) {
            return null;
        }
        return DonationOutboxEventEntity.builder()
                .id(domain.getId())
                .eventId(domain.getEventId())
                .requestId(domain.getRequestId())
                .publicationId(domain.getPublicationId())
                .foundationId(domain.getFoundationId())
                .donorId(domain.getDonorId())
                .rejectionReason(domain.getRejectionReason())
                .eventType(domain.getEventType())
                .status(domain.getStatus())
                .attempts(domain.getAttempts())
                .lastError(domain.getLastError())
                .claimedBy(domain.getClaimedBy())
                .claimedAt(domain.getClaimedAt())
                .occurredAt(domain.getOccurredAt())
                .createdAt(domain.getCreatedAt())
                .sentAt(domain.getSentAt())
                .build();
    }

    public DonationOutboxEvent toDomain(DonationOutboxEventEntity entity) {
        if (entity == null) {
            return null;
        }
        return DonationOutboxEvent.builder()
                .id(entity.getId())
                .eventId(entity.getEventId())
                .requestId(entity.getRequestId())
                .publicationId(entity.getPublicationId())
                .foundationId(entity.getFoundationId())
                .donorId(entity.getDonorId())
                .rejectionReason(entity.getRejectionReason())
                .eventType(entity.getEventType())
                .status(entity.getStatus())
                .attempts(entity.getAttempts())
                .lastError(entity.getLastError())
                .claimedBy(entity.getClaimedBy())
                .claimedAt(entity.getClaimedAt())
                .occurredAt(entity.getOccurredAt())
                .createdAt(entity.getCreatedAt())
                .sentAt(entity.getSentAt())
                .build();
    }
}
