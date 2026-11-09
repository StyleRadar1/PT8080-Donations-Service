package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging.event.DonationRequestCreatedIntegrationEvent;

/**
 * Consumidor simulador representativo del microservicio 'ms-notificaciones' (puerto 8300).
 * Escucha la cola 'q.notificaciones.donacion-solicitud-creada' vinculada a 'donaciones.exchange'.
 * Habilitable mediante 'app.rabbitmq.simulator.enabled=true' para generar evidencia en consola y pruebas.
 */
@Component
@ConditionalOnProperty(name = "app.rabbitmq.simulator.enabled", havingValue = "true", matchIfMissing = false)
public class NotificationConsumerSimulator {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumerSimulator.class);

    @RabbitListener(queues = "${app.rabbitmq.queues.notifications-donation-created:q.notificaciones.donacion-solicitud-creada}")
    public void onDonationRequestCreated(DonationRequestCreatedIntegrationEvent event) {
        log.info("=========================================================================================");
        log.info("🔔 [MS-NOTIFICACIONES :8300 - CONSUMIDOR] Evento de integración recibido desde RabbitMQ");
        log.info("🔔 [MS-NOTIFICACIONES] Tipo de Evento: {}", event.getEventType());
        log.info("🔔 [MS-NOTIFICACIONES] EventId: {}", event.getEventId());
        log.info("🔔 [MS-NOTIFICACIONES] Productor: {}", event.getProducer());
        log.info("🔔 [MS-NOTIFICACIONES] Solicitud ID: {}", event.getRequestId());
        log.info("🔔 [MS-NOTIFICACIONES] Donante ID: {}", event.getDonorId());
        log.info("🔔 [MS-NOTIFICACIONES] Fundación ID: {}", event.getFoundationId());
        log.info("🔔 [MS-NOTIFICACIONES] Publicación ID: {}", event.getPublicationId());
        log.info("🔔 [MS-NOTIFICACIONES] Acción disparada: Generación de alerta y notificación a la fundación {} por postulación del donante {}",
                event.getFoundationId(), event.getDonorId());
        log.info("=========================================================================================");
    }
}
