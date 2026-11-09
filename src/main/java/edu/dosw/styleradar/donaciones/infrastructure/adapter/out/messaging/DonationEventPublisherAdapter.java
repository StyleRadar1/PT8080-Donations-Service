package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import edu.dosw.styleradar.donaciones.domain.event.DonationDeliveredEvent;
import edu.dosw.styleradar.donaciones.domain.event.DonationRequestCreatedEvent;
import edu.dosw.styleradar.donaciones.domain.event.DonationRequestDecidedEvent;
import edu.dosw.styleradar.donaciones.domain.event.PickupScheduledEvent;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationEventPublisherPort;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging.event.DonationRequestCreatedIntegrationEvent;

/**
 * Adaptador de salida RabbitMQ para la publicación asíncrona de eventos hacia el broker (puerto 5672).
 * Emite eventos con confirmaciones de publicación (CorrelationData) hacia 'donaciones.exchange'
 * con la clave de enrutamiento 'donacion.solicitud.creada' para el microservicio ms-notificaciones (puerto 8300).
 */
@Component("donacionEventPublisherAdapter")
public class DonationEventPublisherAdapter implements DonationEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(DonationEventPublisherAdapter.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeName;
    private final String donationRequestCreatedRoutingKey;
    private final String donationStatusUpdatedRoutingKey;

    public DonationEventPublisherAdapter(
            RabbitTemplate rabbitTemplate,
            @Value("${app.rabbitmq.exchange.donations:donaciones.exchange}") String exchangeName,
            @Value("${app.rabbitmq.routing-keys.donation-request-created:donacion.solicitud.creada}") String donationRequestCreatedRoutingKey,
            @Value("${app.rabbitmq.routing-keys.donation-status-updated:donacion.estado.actualizado}") String donationStatusUpdatedRoutingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeName = exchangeName;
        this.donationRequestCreatedRoutingKey = donationRequestCreatedRoutingKey;
        this.donationStatusUpdatedRoutingKey = donationStatusUpdatedRoutingKey;
    }

    @Override
    public void publishEvent(Object event) {
        if (event == null) {
            log.warn("Se omitió publicación de evento nulo hacia RabbitMQ.");
            return;
        }

        try {
            if (event instanceof DonationRequestCreatedEvent domainEvent) {
                publishDonationRequestCreated(domainEvent);
            } else if (event instanceof DonationRequestDecidedEvent
                    || event instanceof PickupScheduledEvent
                    || event instanceof DonationDeliveredEvent) {
                publishGenericStatusEvent(event);
            } else {
                log.info("Publicando evento genérico en broker RabbitMQ [Exchange: {}, Evento: {}]",
                        exchangeName, event.getClass().getSimpleName());
                CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
                rabbitTemplate.convertAndSend(exchangeName, "donaciones.general", event, correlationData);
            }
        } catch (AmqpException ex) {
            log.error("Fallo crítico al publicar evento en RabbitMQ [Exchange: {}, Evento: {}]: {}. Verifique conectividad con el broker.",
                    exchangeName, event.getClass().getSimpleName(), ex.getMessage(), ex);
            throw ex;
        }
    }

    private void publishDonationRequestCreated(DonationRequestCreatedEvent domainEvent) {
        DonationRequestCreatedIntegrationEvent integrationEvent = DonationRequestCreatedIntegrationEvent.from(domainEvent);
        CorrelationData correlationData = new CorrelationData(integrationEvent.getEventId());

        log.info(">>> [RabbitMQ PUBLISH] Publicando evento '{}' hacia Exchange '{}' con Routing Key '{}' para ms-notificaciones (8300): {}",
                integrationEvent.getEventType(), exchangeName, donationRequestCreatedRoutingKey, integrationEvent);

        rabbitTemplate.convertAndSend(exchangeName, donationRequestCreatedRoutingKey, integrationEvent, correlationData);

        log.info("<<< [RabbitMQ PUBLISHED] Evento '{}' enviado exitosamente al broker (EventId: {}, SolicitudId: {}, DonanteId: {}, FundaciónId: {})",
                integrationEvent.getEventType(), integrationEvent.getEventId(), domainEvent.requestId(), domainEvent.donorId(), domainEvent.foundationId());
    }

    private void publishGenericStatusEvent(Object event) {
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        log.info(">>> [RabbitMQ PUBLISH] Publicando actualización de estado hacia Exchange '{}' con Routing Key '{}': {}",
                exchangeName, donationStatusUpdatedRoutingKey, event);
        rabbitTemplate.convertAndSend(exchangeName, donationStatusUpdatedRoutingKey, event, correlationData);
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public String getDonationRequestCreatedRoutingKey() {
        return donationRequestCreatedRoutingKey;
    }

    public String getDonationStatusUpdatedRoutingKey() {
        return donationStatusUpdatedRoutingKey;
    }
}
