package edu.dosw.styleradar.donaciones.application.usecase;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import edu.dosw.styleradar.donaciones.domain.event.DonationRequestDecidedEvent;
import edu.dosw.styleradar.donaciones.domain.exception.ExternalServiceUnavailableException;
import edu.dosw.styleradar.donaciones.domain.model.DonationOutboxEvent;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationOutboxRepositoryPort;

/**
 * Despachador de eventos pendientes del Outbox transaccional.
 * Reclama cada registro atómicamente antes de publicar, exige confirmación efectiva (ACK) del broker
 * antes de marcar {@code SENT} y conserva el mismo {@code eventId} en todos los reintentos.
 */
@Service
public class DonationOutboxDispatcherService {

    private static final Logger log = LoggerFactory.getLogger(DonationOutboxDispatcherService.class);
    private static final long DEFAULT_CLAIM_LEASE_SECONDS = 60L;

    private final DonationOutboxRepositoryPort outboxRepositoryPort;
    private final DonationEventPublisherPort eventPublisherPort;
    private final Duration claimLease;

    @Autowired
    public DonationOutboxDispatcherService(
            DonationOutboxRepositoryPort outboxRepositoryPort,
            DonationEventPublisherPort eventPublisherPort,
            @Value("${app.outbox.claim-lease-seconds:60}") long claimLeaseSeconds) {
        this.outboxRepositoryPort = outboxRepositoryPort;
        this.eventPublisherPort = eventPublisherPort;
        this.claimLease = Duration.ofSeconds(Math.max(0L, claimLeaseSeconds));
    }

    public DonationOutboxDispatcherService(
            DonationOutboxRepositoryPort outboxRepositoryPort,
            DonationEventPublisherPort eventPublisherPort) {
        this(outboxRepositoryPort, eventPublisherPort, DEFAULT_CLAIM_LEASE_SECONDS);
    }

    public int dispatchPendingByFoundationId(Long foundationId) {
        if (foundationId == null) {
            return 0;
        }
        LocalDateTime staleBefore = LocalDateTime.now(ZoneOffset.UTC).minus(claimLease);
        return dispatchEvents(outboxRepositoryPort.findDispatchableByFoundationId(foundationId, staleBefore));
    }

    public int dispatchAllPending() {
        LocalDateTime staleBefore = LocalDateTime.now(ZoneOffset.UTC).minus(claimLease);
        return dispatchEvents(outboxRepositoryPort.findDispatchableEvents(staleBefore));
    }

    private int dispatchEvents(List<DonationOutboxEvent> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return 0;
        }

        int sentCount = 0;
        int failedCount = 0;
        RuntimeException lastFailure = null;
        String workerClaimPrefix = UUID.randomUUID().toString();

        for (DonationOutboxEvent candidate : candidates) {
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            LocalDateTime staleBefore = now.minus(claimLease);
            String claimToken = workerClaimPrefix + ":" + candidate.getId();

            Optional<DonationOutboxEvent> claimedOpt = outboxRepositoryPort.claimForDispatch(
                    candidate.getId(), claimToken, now, staleBefore);
            if (claimedOpt.isEmpty()) {
                log.debug("Registro Outbox ID: {} (solicitud {}) ya reclamado o procesado por otro hilo/worker.",
                        candidate.getId(), candidate.getRequestId());
                continue;
            }

            DonationOutboxEvent claimed = claimedOpt.get();
            DonationRequestDecidedEvent domainEvent = new DonationRequestDecidedEvent(
                    claimed.getRequestId(),
                    claimed.getFoundationId(),
                    claimed.getDonorId(),
                    false,
                    claimed.getRejectionReason(),
                    Instant.now(),
                    claimed.getEventId()
            );

            boolean brokerConfirmed = false;
            try {
                eventPublisherPort.publishEventConfirmed(domainEvent);
                brokerConfirmed = true;
                outboxRepositoryPort.markSent(claimed.getId(), claimToken, LocalDateTime.now(ZoneOffset.UTC));
                sentCount++;
            } catch (RuntimeException ex) {
                failedCount++;
                lastFailure = ex;
                if (!brokerConfirmed) {
                    outboxRepositoryPort.releaseForRetry(claimed.getId(), claimToken, ex.getMessage());
                }
                log.error("Fallo en entrega Outbox [solicitudId={}, eventId={}, brokerConfirmed={}]. Queda recuperable para reintento.",
                        claimed.getRequestId(), claimed.getEventId(), brokerConfirmed, ex);
            }
        }

        if (failedCount > 0) {
            throw new ExternalServiceUnavailableException(
                    "Falló el cierre de " + failedCount + " aviso(s) pendiente(s) en el Outbox hacia RabbitMQ.",
                    lastFailure);
        }

        return sentCount;
    }
}
