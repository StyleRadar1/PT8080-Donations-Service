package edu.dosw.styleradar.donaciones.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import edu.dosw.styleradar.donaciones.domain.event.PublicationSoldEvent;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidPublicationException;
import edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException;
import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;
import edu.dosw.styleradar.donaciones.domain.port.in.EditPublicationCommand;
import edu.dosw.styleradar.donaciones.domain.port.in.ManagePublicationUseCase;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicationRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Implementación del caso de uso de gestión (edición, retiro, venta y destinación a donación) de publicaciones.
 */
@Service
@RequiredArgsConstructor
public class ManagePublicationUseCaseImpl implements ManagePublicationUseCase {

    private static final Logger log = LoggerFactory.getLogger(ManagePublicationUseCaseImpl.class);

    private final PublicationRepositoryPort publicationRepositoryPort;
    private final DonationEventPublisherPort eventPublisherPort;

    @Override
    public SecondHandPublication edit(EditPublicationCommand command) {
        log.info("Editando publicación ID: {} por usuario ID: {}", command.publicationId(), command.userId());
        SecondHandPublication publication = getAndValidateOwner(command.publicationId(), command.userId());

        publication.edit(
                command.garment(),
                command.size(),
                command.conservationStatus(),
                command.price(),
                command.photos()
        );

        SecondHandPublication saved = publicationRepositoryPort.save(publication);
        log.info("Publicación ID: {} actualizada con éxito", saved.getId());
        return saved;
    }

    @Override
    public SecondHandPublication markAsSold(Long userId, Long publicationId) {
        return markAsSold(userId, publicationId, null);
    }

    @Override
    public SecondHandPublication markAsSold(Long userId, Long publicationId, Long compradorId) {
        log.info("Marcando como vendida publicación ID: {} por usuario ID: {} (compradorId: {})",
                publicationId, userId, compradorId);
        SecondHandPublication publication = getAndValidateOwner(publicationId, userId);

        publication.markAsSold();
        SecondHandPublication saved = publicationRepositoryPort.save(publication);

        eventPublisherPort.publishEvent(new PublicationSoldEvent(saved.getId(), saved.getUserId(), compradorId));
        log.info("Publicación ID: {} marcada como VENDIDA exitosamente", saved.getId());
        return saved;
    }

    @Override
    public void retire(Long userId, Long publicationId) {
        log.info("Retirando publicación ID: {} por usuario ID: {}", publicationId, userId);
        SecondHandPublication publication = getAndValidateOwner(publicationId, userId);

        publication.retire();
        publicationRepositoryPort.save(publication);
        log.info("Publicación ID: {} retirada exitosamente", publicationId);
    }

    @Override
    public SecondHandPublication markForDonation(Long userId, Long publicationId) {
        log.info("Destinando publicación ID: {} hacia canal de donación", publicationId);
        SecondHandPublication publication = getAndValidateOwner(publicationId, userId);

        publication.markForDonation();
        SecondHandPublication saved = publicationRepositoryPort.save(publication);
        log.info("Publicación ID: {} en estado IN_DONATION", saved.getId());
        return saved;
    }

    private SecondHandPublication getAndValidateOwner(Long publicationId, Long userId) {
        SecondHandPublication publication = publicationRepositoryPort.findById(publicationId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe publicación con ID: " + publicationId));

        if (!publication.getUserId().equals(userId)) {
            throw new InvalidPublicationException("La publicación no pertenece al usuario especificado");
        }
        return publication;
    }
}
