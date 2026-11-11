package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;

/**
 * Adaptador de compatibilidad retroactiva.
 * Adaptador de compatibilidad para delegar en {@link DonationEventPublisherAdapter}.
 */
public class RabbitMQEventPublisherAdapter extends DonationEventPublisherAdapter {

    public RabbitMQEventPublisherAdapter(
            RabbitTemplate rabbitTemplate,
            @Value("${app.rabbitmq.exchange.donations:donaciones.exchange}") String exchangeName,
            @Value("${app.rabbitmq.routing-keys.donation-request-created:donacion.solicitud.creada}") String donationRequestCreatedRoutingKey,
            @Value("${app.rabbitmq.routing-keys.donation-status-updated:donacion.estado.actualizado}") String donationStatusUpdatedRoutingKey) {
        super(rabbitTemplate, exchangeName, donationRequestCreatedRoutingKey, donationStatusUpdatedRoutingKey);
    }
}
