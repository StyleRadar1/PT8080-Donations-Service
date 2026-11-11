package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.entity.DonationRequestEntity;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.entity.SecondHandPublicationEntity;

/**
 * Mapper bidireccional entre la entidad de dominio DonationRequest y la entidad JPA DonationRequestEntity.
 */
@Component
public class DonationRequestPersistenceMapper {

    public DonationRequest toDomain(DonationRequestEntity entity) {
        if (entity == null) {
            return null;
        }

        Long publicationId = entity.getPublication() != null ? entity.getPublication().getId() : null;

        return DonationRequest.builder()
                .id(entity.getId())
                .publicationId(publicationId)
                .donorId(entity.getDonorId())
                .foundationId(entity.getFoundationId())
                .message(entity.getMessage())
                .status(entity.getStatus())
                .rejectionReason(entity.getRejectionReason())
                .deliveryPoint(entity.getDeliveryPoint())
                .pickupStartTime(entity.getPickupStartTime())
                .pickupEndTime(entity.getPickupEndTime())
                .version(entity.getVersion())
                .requestedAt(entity.getRequestedAt())
                .decidedAt(entity.getDecidedAt())
                .receivedAt(entity.getReceivedAt())
                .build();
    }

    public DonationRequestEntity toEntity(DonationRequest domain, SecondHandPublicationEntity publication) {
        if (domain == null) {
            return null;
        }

        return DonationRequestEntity.builder()
                .id(domain.getId())
                .publication(publication)
                .donorId(domain.getDonorId())
                .foundationId(domain.getFoundationId())
                .message(domain.getMessage())
                .status(domain.getStatus())
                .rejectionReason(domain.getRejectionReason())
                .deliveryPoint(domain.getDeliveryPoint())
                .pickupStartTime(domain.getPickupStartTime())
                .pickupEndTime(domain.getPickupEndTime())
                .version(domain.getVersion())
                .requestedAt(domain.getRequestedAt())
                .decidedAt(domain.getDecidedAt())
                .receivedAt(domain.getReceivedAt())
                .build();
    }
}
