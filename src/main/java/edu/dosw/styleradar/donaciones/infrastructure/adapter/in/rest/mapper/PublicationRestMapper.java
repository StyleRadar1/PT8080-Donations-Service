package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest.mapper;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import edu.dosw.styleradar.donaciones.application.dto.request.CreatePublicationRequestDTO;
import edu.dosw.styleradar.donaciones.application.dto.request.EditPublicationRequestDTO;
import edu.dosw.styleradar.donaciones.application.dto.response.PublicationResponseDTO;
import edu.dosw.styleradar.donaciones.domain.model.Garment;
import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;
import edu.dosw.styleradar.donaciones.domain.port.in.CreatePublicationCommand;
import edu.dosw.styleradar.donaciones.domain.port.in.EditPublicationCommand;

/**
 * Mapper para transformar entre los DTOs de la API REST y los comandos/agregados del dominio para publicaciones.
 */
@Component
public class PublicationRestMapper {

    public CreatePublicationCommand toCommand(CreatePublicationRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        Garment garment = Garment.builder()
                .name(dto.garmentName())
                .description(dto.description())
                .type(dto.garmentType())
                .brand(dto.brand())
                .color(dto.color())
                .style(dto.style())
                .build();

        return new CreatePublicationCommand(
                dto.userId(),
                garment,
                dto.size(),
                dto.conservationStatus(),
                dto.price(),
                dto.photos() != null ? dto.photos() : Collections.emptyList()
        );
    }

    public EditPublicationCommand toCommand(Long publicationId, EditPublicationRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        Garment garment = Garment.builder()
                .name(dto.garmentName())
                .description(dto.description())
                .type(dto.garmentType())
                .brand(dto.brand())
                .color(dto.color())
                .style(dto.style())
                .build();

        return new EditPublicationCommand(
                dto.userId(),
                publicationId,
                garment,
                dto.size(),
                dto.conservationStatus(),
                dto.price(),
                dto.photos() != null ? dto.photos() : Collections.emptyList()
        );
    }

    public PublicationResponseDTO toResponse(SecondHandPublication domain) {
        if (domain == null) {
            return null;
        }

        return new PublicationResponseDTO(
                domain.getId(),
                domain.getUserId(),
                domain.getGarment() != null ? domain.getGarment().getName() : null,
                domain.getGarment() != null ? domain.getGarment().getDescription() : null,
                domain.getGarment() != null ? domain.getGarment().getType() : null,
                domain.getGarment() != null ? domain.getGarment().getBrand() : null,
                domain.getGarment() != null ? domain.getGarment().getColor() : null,
                domain.getGarment() != null ? domain.getGarment().getStyle() : null,
                domain.getSize(),
                domain.getConservationStatus(),
                domain.getPrice(),
                domain.getStatus(),
                domain.getPublishedAt(),
                domain.getUpdatedAt(),
                domain.getPhotos() != null ? domain.getPhotos() : Collections.emptyList()
        );
    }

    public List<PublicationResponseDTO> toResponseList(List<SecondHandPublication> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        return list.stream().map(this::toResponse).toList();
    }
}
