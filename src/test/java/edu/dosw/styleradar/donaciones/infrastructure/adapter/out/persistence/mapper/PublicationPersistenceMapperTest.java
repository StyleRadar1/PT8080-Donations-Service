package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.dosw.styleradar.donaciones.domain.model.ConservationStatus;
import edu.dosw.styleradar.donaciones.domain.model.Garment;
import edu.dosw.styleradar.donaciones.domain.model.GarmentType;
import edu.dosw.styleradar.donaciones.domain.model.PublicationStatus;
import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;
import edu.dosw.styleradar.donaciones.domain.model.Size;
import edu.dosw.styleradar.donaciones.domain.model.Style;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.entity.SecondHandPublicationEntity;

import static org.assertj.core.api.Assertions.assertThat;

class PublicationPersistenceMapperTest {

    private final PublicationPersistenceMapper mapper = new PublicationPersistenceMapper();

    @Test
    @DisplayName("Debe manejar nulos al convertir entre entidad y dominio")
    void shouldHandleNulls() {
        assertThat(mapper.toDomain(null)).isNull();
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    @DisplayName("Debe mapear correctamente un objeto SecondHandPublication a SecondHandPublicationEntity y viceversa")
    void shouldMapBidirectionally() {
        Garment garment = Garment.builder()
                .name("Chaqueta")
                .description("Desc")
                .type(GarmentType.UPPER)
                .brand("Brand")
                .color("Negro")
                .style(Style.CASUAL)
                .build();

        SecondHandPublication domain = SecondHandPublication.builder()
                .id(1L)
                .userId(10L)
                .garment(garment)
                .size(Size.L)
                .conservationStatus(ConservationStatus.GOOD_CONDITION)
                .price(new BigDecimal("99000.00"))
                .status(PublicationStatus.AVAILABLE)
                .publishedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .photos(List.of("https://cdn.test/1.jpg", "https://cdn.test/2.jpg"))
                .build();

        SecondHandPublicationEntity entity = mapper.toEntity(domain);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(domain.getId());
        assertThat(entity.getGarmentName()).isEqualTo("Chaqueta");
        assertThat(entity.getPhotos()).hasSize(2);
        assertThat(entity.getPhotos().get(0).getOrderIndex()).isEqualTo((short) 0);
        assertThat(entity.getPhotos().get(1).getOrderIndex()).isEqualTo((short) 1);

        SecondHandPublication backToDomain = mapper.toDomain(entity);

        assertThat(backToDomain).isNotNull();
        assertThat(backToDomain.getGarment().getName()).isEqualTo("Chaqueta");
        assertThat(backToDomain.getPhotos()).containsExactly("https://cdn.test/1.jpg", "https://cdn.test/2.jpg");
    }

    @Test
    @DisplayName("Debe mapear publicación sin fotos ni garment")
    void shouldMapWithoutPhotosOrGarment() {
        SecondHandPublication domain = SecondHandPublication.builder()
                .id(2L)
                .userId(20L)
                .price(new BigDecimal("50000.00"))
                .status(PublicationStatus.AVAILABLE)
                .build();

        SecondHandPublicationEntity entity = mapper.toEntity(domain);
        assertThat(entity).isNotNull();
        assertThat(entity.getGarmentName()).isNull();
        assertThat(entity.getPhotos()).isEmpty();

        SecondHandPublication back = mapper.toDomain(entity);
        assertThat(back).isNotNull();
        assertThat(back.getPhotos()).isEmpty();
    }
}
