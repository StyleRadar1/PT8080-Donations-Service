package edu.dosw.styleradar.donaciones.application.usecase;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.donaciones.domain.exception.InvalidDonationRequestException;
import edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException;
import edu.dosw.styleradar.donaciones.domain.model.DonationOutboxEvent;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.domain.port.in.ConfirmDonationReceiptUseCase;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationOutboxRepositoryPort;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationRequestRepositoryPort;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicationRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Implementación del caso de uso de confirmación de recepción de prendas donadas en la sede de la fundación.
 * Registra en la misma transacción dos eventos independientes en el Outbox transaccional:
 * uno para {@code ms-notificaciones} y otro para {@code ms-estadisticas}.
 */
@Service
@RequiredArgsConstructor
public class ConfirmDonationReceiptUseCaseImpl implements ConfirmDonationReceiptUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConfirmDonationReceiptUseCaseImpl.class);

    private final DonationRequestRepositoryPort donationRequestRepositoryPort;
    private final PublicationRepositoryPort publicationRepositoryPort;
    private final DonationOutboxRepositoryPort outboxRepositoryPort;
    private final DonationOutboxDispatcherService outboxDispatcherService;

    @Override
    @Transactional
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

        Instant occurredAt = (saved.getReceivedAt() != null
                ? saved.getReceivedAt().atOffset(java.time.ZoneOffset.UTC).toInstant()
                : Instant.now()).truncatedTo(ChronoUnit.SECONDS);

        enqueueIfAbsent(DonationOutboxEvent.pendingDeliveryNotifications(saved, occurredAt));
        enqueueIfAbsent(DonationOutboxEvent.pendingDeliveryStatistics(saved, occurredAt));

        TransactionEventHelper.runAfterCommitOrImmediate(() -> dispatchDeliveryOutboxSafely(saved.getId()));

        return saved;
    }

    private void enqueueIfAbsent(DonationOutboxEvent event) {
        if (!outboxRepositoryPort.existsByRequestIdAndEventType(event.getRequestId(), event.getEventType())) {
            outboxRepositoryPort.save(event);
        }
    }

    private void dispatchPending(Long requestId) {
        outboxDispatcherService.dispatchPendingByRequestId(requestId);
    }

    private void dispatchDeliveryOutboxSafely(Long requestId) {
        try {
            dispatchPending(requestId);
        } catch (RuntimeException ex) {
            log.warn("Entrega de donación confirmada [solicitudId={}]; despacho Outbox pospuesto para reintento automático: {}",
                    requestId, ex.getMessage());
        }
    }
}
