package edu.dosw.styleradar.donaciones.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import edu.dosw.styleradar.donaciones.domain.event.PublicationCreatedEvent;
import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;
import edu.dosw.styleradar.donaciones.domain.port.in.CreatePublicationCommand;
import edu.dosw.styleradar.donaciones.domain.port.in.CreatePublicationUseCase;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicationRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Implementación del caso de uso de creación de publicaciones de prendas usadas.
 */
@Service
@RequiredArgsConstructor
public class CreatePublicationUseCaseImpl implements CreatePublicationUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreatePublicationUseCaseImpl.class);

    private final PublicationRepositoryPort publicationRepositoryPort;
    private final DonationEventPublisherPort eventPublisherPort;

    @Override
    public SecondHandPublication create(CreatePublicationCommand command) {
        log.info("Iniciando creación de publicación de segunda mano para el usuario: {}", command.userId());

        SecondHandPublication publication = SecondHandPublication.builder()
                .userId(command.userId())
                .garment(command.garment())
                .size(command.size())
                .conservationStatus(command.conservationStatus())
                .price(command.price())
                .photos(command.photos())
                .build();

        publication.publish();

        SecondHandPublication saved = publicationRepositoryPort.save(publication);
        log.info("Publicación de segunda mano creada exitosamente con ID: {}", saved.getId());

        eventPublisherPort.publishEvent(
                new PublicationCreatedEvent(saved.getId(), saved.getUserId(), saved.getPrice())
        );

        return saved;
    }
}
