package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.mapper;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequestStatus;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.entity.DonationRequestEntity;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.entity.SecondHandPublicationEntity;

import static org.assertj.core.api.Assertions.assertThat;

class DonationRequestPersistenceMapperTest {

    private final DonationRequestPersistenceMapper mapper = new DonationRequestPersistenceMapper();

    @Test
    @DisplayName("Debe manejar nulos al convertir entre entidad y dominio")
    void shouldHandleNulls() {
        assertThat(mapper.toDomain(null)).isNull();
        assertThat(mapper.toEntity(null, null)).isNull();
    }

    @Test
    @DisplayName("Debe mapear bidireccionalmente DonationRequest y DonationRequestEntity")
    void shouldMapBidirectionally() {
        SecondHandPublicationEntity publicationEntity = SecondHandPublicationEntity.builder()
                .id(2L)
                .build();

        DonationRequest domain = DonationRequest.builder()
                .id(1L)
                .publicationId(2L)
                .donorId(3L)
                .foundationId(4L)
                .message("Hola")
                .status(DonationRequestStatus.PENDING)
                .rejectionReason(null)
                .deliveryPoint("Cra 7 # 45")
                .pickupStartTime(LocalDateTime.now())
                .pickupEndTime(LocalDateTime.now().plusHours(2))
                .requestedAt(LocalDateTime.now())
                .decidedAt(null)
                .receivedAt(null)
                .build();

        DonationRequestEntity entity = mapper.toEntity(domain, publicationEntity);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getPublication().getId()).isEqualTo(2L);
        assertThat(entity.getDonorId()).isEqualTo(3L);
        assertThat(entity.getFoundationId()).isEqualTo(4L);
        assertThat(entity.getMessage()).isEqualTo("Hola");
        assertThat(entity.getStatus()).isEqualTo(DonationRequestStatus.PENDING);

        DonationRequest back = mapper.toDomain(entity);

        assertThat(back).isNotNull();
        assertThat(back.getId()).isEqualTo(domain.getId());
        assertThat(back.getStatus()).isEqualTo(domain.getStatus());
        assertThat(back.getDeliveryPoint()).isEqualTo(domain.getDeliveryPoint());
        assertThat(back.getPublicationId()).isEqualTo(2L);
    }
}
