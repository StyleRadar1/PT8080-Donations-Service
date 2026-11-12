package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest.mapper;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import edu.dosw.styleradar.donaciones.application.dto.request.ClaimDonationRequestDTO;
import edu.dosw.styleradar.donaciones.application.dto.request.CreateDonationRequestDTO;
import edu.dosw.styleradar.donaciones.application.dto.request.SchedulePickupRequestDTO;
import edu.dosw.styleradar.donaciones.application.dto.response.DonationRequestResponseDTO;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.domain.port.in.ClaimDonationCommand;
import edu.dosw.styleradar.donaciones.domain.port.in.CreateDonationRequestCommand;
import edu.dosw.styleradar.donaciones.domain.port.in.SchedulePickupCommand;

/**
 * Mapper para transformar entre los DTOs de la API REST y los comandos/agregados de solicitudes de donación.
 */
@Component
public class DonationRequestRestMapper {

    public CreateDonationRequestCommand toCommand(CreateDonationRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        return new CreateDonationRequestCommand(
                dto.donorId(),
                dto.publicationId(),
                dto.foundationId(),
                dto.message()
        );
    }

    public ClaimDonationCommand toCommand(ClaimDonationRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        return new ClaimDonationCommand(
                dto.publicationId(),
                dto.foundationId(),
                dto.message()
        );
    }

    public SchedulePickupCommand toCommand(Long requestId, SchedulePickupRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        return new SchedulePickupCommand(
                dto.userId(),
                requestId,
                dto.deliveryPoint(),
                dto.startTime(),
                dto.endTime()
        );
    }

    public DonationRequestResponseDTO toResponse(DonationRequest domain) {
        if (domain == null) {
            return null;
        }

        return new DonationRequestResponseDTO(
                domain.getId(),
                domain.getPublicationId(),
                domain.getDonorId(),
                domain.getFoundationId(),
                domain.getMessage(),
                domain.getStatus(),
                domain.getRejectionReason(),
                domain.getDeliveryPoint(),
                domain.getPickupStartTime(),
                domain.getPickupEndTime(),
                domain.getVersion(),
                domain.getRequestedAt(),
                domain.getDecidedAt(),
                domain.getReceivedAt()
        );
    }

    public List<DonationRequestResponseDTO> toResponseList(List<DonationRequest> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        return list.stream().map(this::toResponse).toList();
    }
}
