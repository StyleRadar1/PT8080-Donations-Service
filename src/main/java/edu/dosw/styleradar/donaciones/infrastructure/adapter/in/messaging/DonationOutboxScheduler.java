package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import edu.dosw.styleradar.donaciones.domain.port.in.HandleFoundationDeletedUseCase;
import lombok.RequiredArgsConstructor;

/**
 * Reintento periódico automático para avisos pendientes en el Outbox transaccional
 * cuando RabbitMQ estuvo temporalmente no disponible.
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "app.outbox.scheduler.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DonationOutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(DonationOutboxScheduler.class);

    private final HandleFoundationDeletedUseCase handleFoundationDeletedUseCase;

    @Scheduled(fixedDelayString = "${app.outbox.retry-delay-ms:30000}")
    public void retryPendingOutboxEvents() {
        try {
            int dispatched = handleFoundationDeletedUseCase.dispatchPendingOutboxEvents();
            if (dispatched > 0) {
                log.info("Outbox scheduler recuperó y entregó {} aviso(s) pendiente(s) hacia RabbitMQ.", dispatched);
            }
        } catch (RuntimeException ex) {
            log.warn("Reintento de Outbox pospuesto por indisponibilidad temporal del broker: {}", ex.getMessage());
        }
    }
}
