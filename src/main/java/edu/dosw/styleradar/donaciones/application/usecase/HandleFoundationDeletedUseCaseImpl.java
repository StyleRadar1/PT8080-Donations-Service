package edu.dosw.styleradar.donaciones.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import edu.dosw.styleradar.donaciones.domain.port.in.HandleFoundationDeletedUseCase;
import lombok.RequiredArgsConstructor;

/**
 * Orquestador del caso de uso {@code foundation.deleted}:
 * 1) Confirma en una transacción las cancelaciones, la liberación de prendas y los registros Outbox.
 * 2) Despacha los avisos pendientes fuera de esa transacción para garantizar recuperación ante fallos de RabbitMQ.
 */
@Service
@RequiredArgsConstructor
public class HandleFoundationDeletedUseCaseImpl implements HandleFoundationDeletedUseCase {

    static final String CANCELLATION_REASON = FoundationDeletionPersistenceService.CANCELLATION_REASON;

    private static final Logger log = LoggerFactory.getLogger(HandleFoundationDeletedUseCaseImpl.class);

    private final FoundationDeletionPersistenceService persistenceService;
    private final DonationOutboxDispatcherService outboxDispatcherService;

    @Override
    public int handleFoundationDeleted(Long foundationId) {
        if (foundationId == null) {
            log.warn("Se omitió procesamiento de eliminación de fundación por foundationId nulo.");
            return 0;
        }

        int cancelledCount = persistenceService.cancelActiveRequestsAndEnqueueOutbox(foundationId);
        outboxDispatcherService.dispatchPendingByFoundationId(foundationId);
        return cancelledCount;
    }

    @Override
    public int dispatchPendingOutboxEvents() {
        return outboxDispatcherService.dispatchAllPending();
    }
}
