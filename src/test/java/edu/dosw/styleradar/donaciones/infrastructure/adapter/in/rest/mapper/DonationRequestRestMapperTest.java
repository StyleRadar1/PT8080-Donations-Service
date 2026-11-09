package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest.mapper;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.dosw.styleradar.donaciones.application.dto.request.CreateDonationRequestDTO;
import edu.dosw.styleradar.donaciones.application.dto.request.SchedulePickupRequestDTO;
import edu.dosw.styleradar.donaciones.application.dto.response.DonationRequestResponseDTO;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequestStatus;
import edu.dosw.styleradar.donaciones.domain.port.in.CreateDonationRequestCommand;
import edu.dosw.styleradar.donaciones.domain.port.in.SchedulePickupCommand;

import static org.assertj.core.api.Assertions.assertThat;

class DonationRequestRestMapperTest {

    private final DonationRequestRestMapper mapper = new DonationRequestRestMapper();

    @Test
    @DisplayName("Debe manejar nulos en conversiones")
    void shouldHandleNulls() {
        assertThat(mapper.toCommand((CreateDonationRequestDTO) null)).isNull();
        assertThat(mapper.toCommand(1L, null)).isNull();
        assertThat(mapper.toResponse(null)).isNull();
        assertThat(mapper.toResponseList(null)).isEmpty();
    }

    @Test
    @DisplayName("Debe mapear CreateDonationRequestDTO a CreateDonationRequestCommand")
    void shouldMapCreateRequestToCommand() {
        CreateDonationRequestDTO dto = new CreateDonationRequestDTO(1L, 101L, 501L, "Mensaje");

        CreateDonationRequestCommand cmd = mapper.toCommand(dto);

        assertThat(cmd.publicationId()).isEqualTo(1L);
        assertThat(cmd.donorId()).isEqualTo(101L);
        assertThat(cmd.foundationId()).isEqualTo(501L);
        assertThat(cmd.message()).isEqualTo("Mensaje");
    }

    @Test
    @DisplayName("Debe mapear SchedulePickupRequestDTO a SchedulePickupCommand")
    void shouldMapSchedulePickupRequestToCommand() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);
        SchedulePickupRequestDTO dto = new SchedulePickupRequestDTO(101L, "Cra 10 # 20", start, end);

        SchedulePickupCommand cmd = mapper.toCommand(5L, dto);

        assertThat(cmd.requestId()).isEqualTo(5L);
        assertThat(cmd.userId()).isEqualTo(101L);
        assertThat(cmd.deliveryPoint()).isEqualTo("Cra 10 # 20");
        assertThat(cmd.startTime()).isEqualTo(start);
        assertThat(cmd.endTime()).isEqualTo(end);
    }

    @Test
    @DisplayName("Debe mapear DonationRequest a DonationRequestResponseDTO")
    void shouldMapDomainToResponse() {
        DonationRequest domain = DonationRequest.builder()
                .id(1L)
                .publicationId(2L)
                .donorId(3L)
                .foundationId(4L)
                .message("Hola")
                .status(DonationRequestStatus.PENDING)
                .deliveryPoint("Punto")
                .requestedAt(LocalDateTime.now())
                .build();

        DonationRequestResponseDTO resp = mapper.toResponse(domain);

        assertThat(resp.id()).isEqualTo(1L);
        assertThat(resp.status()).isEqualTo(DonationRequestStatus.PENDING);
        assertThat(resp.deliveryPoint()).isEqualTo("Punto");
    }
}
