package edu.dosw.styleradar.donaciones.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import edu.dosw.styleradar.donaciones.domain.event.PublicationCreatedEvent;
import edu.dosw.styleradar.donaciones.domain.model.LocalidadBogota;
import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;
import edu.dosw.styleradar.donaciones.domain.port.in.CreatePublicationCommand;
import edu.dosw.styleradar.donaciones.domain.port.in.CreatePublicationUseCase;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicationRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Implementación del caso de uso de creación de publicaciones en el módulo de Economía Circular.
 * Aplica discriminador de donación, restricción geográfica a Bogotá D.C. y publica eventos.
 */
@Service
@RequiredArgsConstructor
public class CreatePublicationUseCaseImpl implements CreatePublicationUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreatePublicationUseCaseImpl.class);

    private final PublicationRepositoryPort publicationRepositoryPort;
    private final DonationEventPublisherPort eventPublisherPort;

    @Override
    public SecondHandPublication create(CreatePublicationCommand command) {
        log.info("Iniciando creación de publicación (esDonacion: {}) en localidad '{}' para el usuario: {}",
                command.esDonacion(), command.localidad(), command.userId());

        SecondHandPublication publication = SecondHandPublication.builder()
                .userId(command.userId())
                .garment(command.garment())
                .size(command.size())
                .conservationStatus(command.conservationStatus())
                .price(command.price())
                .esDonacion(command.esDonacion())
                .localidad(command.localidad() != null ? command.localidad() : LocalidadBogota.CHAPINERO)
                .photos(command.photos())
                .build();

        publication.publish();

        SecondHandPublication saved = publicationRepositoryPort.save(publication);
        log.info("Publicación de economía circular creada exitosamente con ID: {} (esDonacion: {})",
                saved.getId(), saved.isEsDonacion());

        eventPublisherPort.publishEvent(
                new PublicationCreatedEvent(saved.getId(), saved.getUserId(), saved.getPrice())
        );

        return saved;
    }
}
