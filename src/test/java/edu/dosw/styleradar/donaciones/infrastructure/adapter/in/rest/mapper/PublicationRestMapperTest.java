package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.dosw.styleradar.donaciones.application.dto.request.CreatePublicationRequestDTO;
import edu.dosw.styleradar.donaciones.application.dto.request.EditPublicationRequestDTO;
import edu.dosw.styleradar.donaciones.application.dto.response.PublicationResponseDTO;
import edu.dosw.styleradar.donaciones.domain.model.ConservationStatus;
import edu.dosw.styleradar.donaciones.domain.model.Garment;
import edu.dosw.styleradar.donaciones.domain.model.GarmentType;
import edu.dosw.styleradar.donaciones.domain.model.PublicationStatus;
import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;
import edu.dosw.styleradar.donaciones.domain.model.Size;
import edu.dosw.styleradar.donaciones.domain.model.Style;
import edu.dosw.styleradar.donaciones.domain.port.in.CreatePublicationCommand;
import edu.dosw.styleradar.donaciones.domain.port.in.EditPublicationCommand;

import static org.assertj.core.api.Assertions.assertThat;

class PublicationRestMapperTest {

    private final PublicationRestMapper mapper = new PublicationRestMapper();

    @Test
    @DisplayName("Debe manejar nulos en conversiones")
    void shouldHandleNulls() {
        assertThat(mapper.toCommand((CreatePublicationRequestDTO) null)).isNull();
        assertThat(mapper.toCommand(1L, null)).isNull();
        assertThat(mapper.toResponse(null)).isNull();
        assertThat(mapper.toResponseList(null)).isEmpty();
    }

    @Test
    @DisplayName("Debe mapear CreatePublicationRequestDTO a CreatePublicationCommand")
    void shouldMapCreateRequestToCommand() {
        CreatePublicationRequestDTO dto = new CreatePublicationRequestDTO(
                101L, "Chaqueta", "Desc", GarmentType.UPPER, "Zara", "Negro",
                Style.CASUAL, Size.M, ConservationStatus.LIKE_NEW, new BigDecimal("60000.00"),
                List.of("https://cdn.test/1.jpg")
        );

        CreatePublicationCommand cmd = mapper.toCommand(dto);

        assertThat(cmd.userId()).isEqualTo(101L);
        assertThat(cmd.garment().getName()).isEqualTo("Chaqueta");
        assertThat(cmd.price()).isEqualTo(new BigDecimal("60000.00"));
        assertThat(cmd.photos()).containsExactly("https://cdn.test/1.jpg");
    }

    @Test
    @DisplayName("Debe mapear EditPublicationRequestDTO a EditPublicationCommand")
    void shouldMapEditRequestToCommand() {
        EditPublicationRequestDTO dto = new EditPublicationRequestDTO(
                101L, "Chaqueta Editada", "Desc", GarmentType.UPPER, "Zara", "Azul",
                Style.URBAN, Size.L, ConservationStatus.GOOD_CONDITION, new BigDecimal("55000.00"),
                null
        );

        EditPublicationCommand cmd = mapper.toCommand(1L, dto);

        assertThat(cmd.userId()).isEqualTo(101L);
        assertThat(cmd.publicationId()).isEqualTo(1L);
        assertThat(cmd.garment().getName()).isEqualTo("Chaqueta Editada");
        assertThat(cmd.photos()).isEmpty();
    }

    @Test
    @DisplayName("Debe mapear SecondHandPublication a PublicationResponseDTO")
    void shouldMapDomainToResponse() {
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
                .photos(List.of("https://cdn.test/1.jpg"))
                .build();

        PublicationResponseDTO resp = mapper.toResponse(domain);

        assertThat(resp.id()).isEqualTo(1L);
        assertThat(resp.garmentName()).isEqualTo("Chaqueta");
        assertThat(resp.photos()).containsExactly("https://cdn.test/1.jpg");
    }
}
