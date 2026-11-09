package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import edu.dosw.styleradar.donaciones.domain.port.out.DonationEventPublisherPort;

/**
 * Adaptador de salida para la publicación de eventos de dominio hacia el broker de mensajería (RabbitMQ).
 */
@Component
public class RabbitMQEventPublisherAdapter implements DonationEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQEventPublisherAdapter.class);

    @Override
    public void publishEvent(Object event) {
        log.info("Publicando evento de dominio: {}", event);
    }
}
