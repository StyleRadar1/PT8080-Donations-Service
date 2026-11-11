package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;

/**
 * Adaptador de publicación de eventos con nomenclatura en español conforme a la especificación
 * de entrega de la Fase 3 ("DonacionEventPublisherAdapter").
 * Hereda íntegramente la implementación de {@link DonationEventPublisherAdapter}.
 */
public class DonacionEventPublisherAdapter extends DonationEventPublisherAdapter {

    public DonacionEventPublisherAdapter(
            RabbitTemplate rabbitTemplate,
            @Value("${app.rabbitmq.exchange.donations:donaciones.exchange}") String exchangeName,
            @Value("${app.rabbitmq.routing-keys.donation-request-created:donacion.solicitud.creada}") String donationRequestCreatedRoutingKey,
            @Value("${app.rabbitmq.routing-keys.donation-status-updated:donacion.estado.actualizado}") String donationStatusUpdatedRoutingKey) {
        super(rabbitTemplate, exchangeName, donationRequestCreatedRoutingKey, donationStatusUpdatedRoutingKey);
    }
}
