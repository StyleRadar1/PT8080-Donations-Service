package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import edu.dosw.styleradar.donaciones.domain.model.Garment;
import edu.dosw.styleradar.donaciones.domain.model.LocalidadBogota;
import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.entity.PublicationPhotoEntity;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.entity.SecondHandPublicationEntity;

/**
 * Mapper bidireccional entre el agregado SecondHandPublication y la entidad JPA SecondHandPublicationEntity.
 * Incluye mapeo de 'esDonacion', 'localidad' (Bogotá) y control de versiones optimista.
 */
@Component
public class PublicationPersistenceMapper {

    public SecondHandPublication toDomain(SecondHandPublicationEntity entity) {
        if (entity == null) {
            return null;
        }

        Garment garment = Garment.builder()
                .name(entity.getGarmentName())
                .description(entity.getDescription())
                .type(entity.getGarmentType())
                .brand(entity.getBrand())
                .color(entity.getColor())
                .style(entity.getStyle())
                .build();

        List<String> photoUrls = entity.getPhotos() != null
                ? entity.getPhotos().stream().map(PublicationPhotoEntity::getPhotoUrl).toList()
                : Collections.emptyList();

        return SecondHandPublication.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .garment(garment)
                .size(entity.getSize())
                .conservationStatus(entity.getConservationStatus())
                .price(entity.getPrice())
                .esDonacion(entity.isEsDonacion())
                .localidad(entity.getLocalidad() != null ? entity.getLocalidad() : LocalidadBogota.CHAPINERO)
                .status(entity.getStatus())
                .version(entity.getVersion())
                .publishedAt(entity.getPublishedAt())
                .updatedAt(entity.getUpdatedAt())
                .photos(new ArrayList<>(photoUrls))
                .build();
    }

    public SecondHandPublicationEntity toEntity(SecondHandPublication domain) {
        if (domain == null) {
            return null;
        }

        SecondHandPublicationEntity entity = SecondHandPublicationEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .garmentName(domain.getGarment() != null ? domain.getGarment().getName() : null)
                .description(domain.getGarment() != null ? domain.getGarment().getDescription() : null)
                .garmentType(domain.getGarment() != null ? domain.getGarment().getType() : null)
                .brand(domain.getGarment() != null ? domain.getGarment().getBrand() : null)
                .color(domain.getGarment() != null ? domain.getGarment().getColor() : null)
                .style(domain.getGarment() != null ? domain.getGarment().getStyle() : null)
                .size(domain.getSize())
                .conservationStatus(domain.getConservationStatus())
                .price(domain.getPrice())
                .esDonacion(domain.isEsDonacion())
                .localidad(domain.getLocalidad() != null ? domain.getLocalidad() : LocalidadBogota.CHAPINERO)
                .status(domain.getStatus())
                .version(domain.getVersion())
                .publishedAt(domain.getPublishedAt())
                .updatedAt(domain.getUpdatedAt())
                .photos(new ArrayList<>())
                .build();

        if (domain.getPhotos() != null) {
            for (int i = 0; i < domain.getPhotos().size(); i++) {
                entity.addPhoto(domain.getPhotos().get(i), i);
            }
        }

        return entity;
    }
}
