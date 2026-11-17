package edu.dosw.styleradar.donaciones.application.usecase;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import edu.dosw.styleradar.donaciones.domain.port.out.DonationEventPublisherPort;

/**
 * Utilidad compartida para publicar eventos de dominio tras confirmar el commit en base de datos,
 * o de forma inmediata cuando no existe una sincronización transaccional activa.
 */
final class TransactionEventHelper {

    private TransactionEventHelper() {
    }

    static void publishAfterCommitOrImmediate(DonationEventPublisherPort publisher, Object event) {
        if (event == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publisher.publishEvent(event);
                }
            });
        } else {
            publisher.publishEvent(event);
        }
    }
}
