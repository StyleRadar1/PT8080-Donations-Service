package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;

/**
 * Adaptador de compatibilidad retroactiva.
 * @deprecated Utilizar {@link DonationEventPublisherAdapter} o {@link DonacionEventPublisherAdapter}.
 */
@Deprecated
public class RabbitMQEventPublisherAdapter extends DonationEventPublisherAdapter {

    public RabbitMQEventPublisherAdapter(
            RabbitTemplate rabbitTemplate,
            @Value("${app.rabbitmq.exchange.donations:donaciones.exchange}") String exchangeName,
            @Value("${app.rabbitmq.routing-keys.donation-request-created:donacion.solicitud.creada}") String donationRequestCreatedRoutingKey,
            @Value("${app.rabbitmq.routing-keys.donation-status-updated:donacion.estado.actualizado}") String donationStatusUpdatedRoutingKey) {
        super(rabbitTemplate, exchangeName, donationRequestCreatedRoutingKey, donationStatusUpdatedRoutingKey);
    }
}
