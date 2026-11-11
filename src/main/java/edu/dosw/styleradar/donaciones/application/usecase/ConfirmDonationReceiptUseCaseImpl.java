package edu.dosw.styleradar.donaciones.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import edu.dosw.styleradar.donaciones.domain.event.DonationDeliveredEvent;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidDonationRequestException;
import edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.domain.port.in.ConfirmDonationReceiptUseCase;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationRequestRepositoryPort;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicationRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Implementación del caso de uso de confirmación de recepción de prendas donadas en la sede de la fundación.
 */
@Service
@RequiredArgsConstructor
public class ConfirmDonationReceiptUseCaseImpl implements ConfirmDonationReceiptUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConfirmDonationReceiptUseCaseImpl.class);

    private final DonationRequestRepositoryPort donationRequestRepositoryPort;
    private final PublicationRepositoryPort publicationRepositoryPort;
    private final DonationEventPublisherPort eventPublisherPort;

    @Override
    public DonationRequest confirmReceipt(Long foundationId, Long requestId) {
        log.info("Fundación ID: {} confirma recepción de la donación para la solicitud ID: {}",
                foundationId, requestId);

        DonationRequest request = donationRequestRepositoryPort.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe solicitud de donación con ID: " + requestId));

        if (!request.getFoundationId().equals(foundationId)) {
            throw new InvalidDonationRequestException("La solicitud no corresponde a la fundación indicada");
        }

        request.confirmDelivery();

        publicationRepositoryPort.findById(request.getPublicationId()).ifPresent(publication -> {
            publication.markAsDonated();
            publicationRepositoryPort.save(publication);
            log.info("Publicación ID: {} actualizada a estado DONATED", publication.getId());
        });

        DonationRequest saved = donationRequestRepositoryPort.save(request);
        log.info("Solicitud ID: {} confirmada y marcada como DELIVERED", saved.getId());

        eventPublisherPort.publishEvent(
                new DonationDeliveredEvent(
                        saved.getId(),
                        saved.getPublicationId(),
                        saved.getFoundationId(),
                        saved.getDonorId()
                )
        );

        return saved;
    }
}
