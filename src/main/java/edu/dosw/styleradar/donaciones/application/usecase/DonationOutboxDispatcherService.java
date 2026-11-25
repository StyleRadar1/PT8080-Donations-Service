package edu.dosw.styleradar.donaciones.application.usecase;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import edu.dosw.styleradar.donaciones.domain.event.DonationRequestDecidedEvent;
import edu.dosw.styleradar.donaciones.domain.exception.ExternalServiceUnavailableException;
import edu.dosw.styleradar.donaciones.domain.model.DonationOutboxEvent;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationOutboxRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Despachador de eventos pendientes del Outbox transaccional.
 * Procesa cada registro por solicitud de forma independiente para que el éxito
 * de un aviso quede confirmado aunque falle la entrega de otro posterior.
 */
@Service
@RequiredArgsConstructor
public class DonationOutboxDispatcherService {

    private static final Logger log = LoggerFactory.getLogger(DonationOutboxDispatcherService.class);

    private final DonationOutboxRepositoryPort outboxRepositoryPort;
    private final DonationEventPublisherPort eventPublisherPort;

    public int dispatchPendingByFoundationId(Long foundationId) {
        if (foundationId == null) {
            return 0;
        }
        return dispatchEvents(outboxRepositoryPort.findPendingByFoundationId(foundationId));
    }

    public int dispatchAllPending() {
        return dispatchEvents(outboxRepositoryPort.findPendingEvents());
    }

    private int dispatchEvents(List<DonationOutboxEvent> pendingEvents) {
        if (pendingEvents == null || pendingEvents.isEmpty()) {
            return 0;
        }

        int sentCount = 0;
        int failedCount = 0;
        RuntimeException lastFailure = null;

        for (DonationOutboxEvent outboxEvent : pendingEvents) {
            try {
                DonationRequestDecidedEvent domainEvent = new DonationRequestDecidedEvent(
                        outboxEvent.getRequestId(),
                        outboxEvent.getFoundationId(),
                        outboxEvent.getDonorId(),
                        false,
                        outboxEvent.getRejectionReason()
                );
                eventPublisherPort.publishEvent(domainEvent);
                outboxEvent.markSent();
                outboxRepositoryPort.save(outboxEvent);
                sentCount++;
            } catch (RuntimeException ex) {
                failedCount++;
                lastFailure = ex;
                outboxEvent.recordFailure(ex.getMessage());
                outboxRepositoryPort.save(outboxEvent);
                log.error("Fallo al despachar aviso Outbox para solicitud ID: {} (intento #{}). Queda PENDING para reintento.",
                        outboxEvent.getRequestId(), outboxEvent.getAttempts(), ex);
            }
        }

        if (failedCount > 0) {
            throw new ExternalServiceUnavailableException(
                    "Falló la entrega de " + failedCount + " aviso(s) pendiente(s) en el Outbox hacia RabbitMQ.",
                    lastFailure);
        }

        return sentCount;
    }
}
