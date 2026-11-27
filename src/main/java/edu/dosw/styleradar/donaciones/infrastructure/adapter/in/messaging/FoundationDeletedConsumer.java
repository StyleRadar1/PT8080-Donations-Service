package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import edu.dosw.styleradar.donaciones.domain.port.in.HandleFoundationDeletedUseCase;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.in.messaging.event.FoundationDeletedIntegrationEvent;
import lombok.RequiredArgsConstructor;

/**
 * Consumidor RabbitMQ para el evento {@code foundation.deleted} emitido por {@code ms-tiendas}.
 * Escucha la cola durable {@code styleradar.donaciones.foundation-deleted.q} y cierra
 * automáticamente cualquier solicitud activa asociada a la fundación eliminada.
 */
@Component
@RequiredArgsConstructor
public class FoundationDeletedConsumer {

    private static final Logger log = LoggerFactory.getLogger(FoundationDeletedConsumer.class);

    private final HandleFoundationDeletedUseCase handleFoundationDeletedUseCase;

    @RabbitListener(queues = "${app.rabbitmq.queues.foundation-deleted:styleradar.donaciones.foundation-deleted.q}")
    public void onFoundationDeleted(FoundationDeletedIntegrationEvent event) {
        if (event == null || event.getFoundationId() == null) {
            log.error("Evento 'foundation.deleted' inválido recibido (falta foundationId). Se envía a DLQ.");
            throw new AmqpRejectAndDontRequeueException(
                    "Evento foundation.deleted inválido: el campo foundationId es obligatorio");
        }

        log.info("<<< [RabbitMQ CONSUME] Recibido 'foundation.deleted' [eventId={}, foundationId={}, deletedAt={}]",
                event.getEventId(), event.getFoundationId(), event.getDeletedAt());

        int cancelled = handleFoundationDeletedUseCase.handleFoundationDeleted(event.getFoundationId());

        log.info("<<< [RabbitMQ CONSUME] Procesamiento 'foundation.deleted' completado [foundationId={}, solicitudesCerradas={}]",
                event.getFoundationId(), cancelled);
    }
}
