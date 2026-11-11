package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import edu.dosw.styleradar.donaciones.domain.event.DonationDeliveredEvent;
import edu.dosw.styleradar.donaciones.domain.event.DonationRequestCreatedEvent;
import edu.dosw.styleradar.donaciones.domain.event.DonationRequestDecidedEvent;
import edu.dosw.styleradar.donaciones.domain.event.PickupScheduledEvent;
import edu.dosw.styleradar.donaciones.domain.event.PublicationSoldEvent;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationEventPublisherPort;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging.event.DonationRequestCreatedIntegrationEvent;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging.event.SegundaVidaDonacionEstadoCambiadoIntegrationEvent;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging.event.SegundaVidaDonacionPrendaVendidaIntegrationEvent;

/**
 * Adaptador de salida RabbitMQ para la publicación asíncrona hacia 'styleradar.exchange' (puerto 5671 TLS / 5672).
 * Ambos flujos pertenecen a 'ms-segunda-vida-donaciones':
 * 1) 'styleradar.donaciones.estado.cambiado' hacia ms-notificaciones y ms-estadisticas.
 * 2) 'styleradar.segundavida.prenda.vendida' hacia ms-chat (cola 'q.chat.segunda-mano') para cierre de salas.
 */
@Component("donacionEventPublisherAdapter")
public class DonationEventPublisherAdapter implements DonationEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(DonationEventPublisherAdapter.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeName;
    private final String donationStatusUpdatedRoutingKey;
    private final String secondHandSoldRoutingKey;
    private final String donationRequestCreatedRoutingKey;

    public DonationEventPublisherAdapter(
            RabbitTemplate rabbitTemplate,
            @Value("${app.rabbitmq.exchange.main:${app.rabbitmq.exchange.donations:styleradar.exchange}}") String exchangeName,
            @Value("${app.rabbitmq.routing-keys.donation-status-updated:styleradar.donaciones.estado.cambiado}") String donationStatusUpdatedRoutingKey,
            @Value("${app.rabbitmq.routing-keys.second-hand-sold:styleradar.segundavida.prenda.vendida}") String secondHandSoldRoutingKey,
            @Value("${app.rabbitmq.routing-keys.donation-request-created:styleradar.donaciones.estado.cambiado}") String donationRequestCreatedRoutingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeName = exchangeName;
        this.donationStatusUpdatedRoutingKey = donationStatusUpdatedRoutingKey;
        this.secondHandSoldRoutingKey = secondHandSoldRoutingKey;
        this.donationRequestCreatedRoutingKey = donationRequestCreatedRoutingKey;
    }

    public DonationEventPublisherAdapter(
            RabbitTemplate rabbitTemplate,
            String exchangeName,
            String donationRequestCreatedRoutingKey,
            String donationStatusUpdatedRoutingKey) {
        this(rabbitTemplate, exchangeName, donationStatusUpdatedRoutingKey, "styleradar.segundavida.prenda.vendida", donationRequestCreatedRoutingKey);
    }

    @Override
    public void publishEvent(Object event) {
        if (event == null) {
            log.warn("Se omitió publicación de evento nulo hacia RabbitMQ.");
            return;
        }

        if (event instanceof DonationRequestCreatedEvent domainEvent) {
            publishDonationRequestCreated(domainEvent);
        } else if (event instanceof PublicationSoldEvent soldEvent) {
            publishPublicationSold(soldEvent);
        } else if (event instanceof DonationRequestDecidedEvent decidedEvent) {
            publishDonationDecided(decidedEvent);
        } else if (event instanceof PickupScheduledEvent pickupEvent) {
            publishPickupScheduled(pickupEvent);
        } else if (event instanceof DonationDeliveredEvent deliveredEvent) {
            publishDonationDelivered(deliveredEvent);
        } else {
            log.info("Publicando evento en broker RabbitMQ [Exchange: {}, Evento: {}]",
                    exchangeName, event.getClass().getSimpleName());
            CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
            rabbitTemplate.convertAndSend(exchangeName, donationStatusUpdatedRoutingKey, event, correlationData);
        }
    }

    private void publishDonationRequestCreated(DonationRequestCreatedEvent domainEvent) {
        DonationRequestCreatedIntegrationEvent integrationEvent = DonationRequestCreatedIntegrationEvent.from(domainEvent);
        CorrelationData correlationData = new CorrelationData(integrationEvent.getEventId());

        log.info(">>> [RabbitMQ PUBLISH] Publicando '{}' desde ms-segunda-vida-donaciones hacia Exchange '{}' con Routing Key '{}'",
                integrationEvent.getEventType(), exchangeName, donationRequestCreatedRoutingKey);

        rabbitTemplate.convertAndSend(exchangeName, donationRequestCreatedRoutingKey, integrationEvent, correlationData);
    }

    private void publishPublicationSold(PublicationSoldEvent domainEvent) {
        SegundaVidaDonacionPrendaVendidaIntegrationEvent event = SegundaVidaDonacionPrendaVendidaIntegrationEvent.from(domainEvent);
        CorrelationData correlationData = new CorrelationData(event.getEventoId());

        log.info(">>> [RabbitMQ PUBLISH] Publicando venta C2C '{}' desde ms-segunda-vida-donaciones hacia Exchange '{}' con Routing Key '{}' para ms-chat",
                event.getTipoEvento(), exchangeName, secondHandSoldRoutingKey);

        rabbitTemplate.convertAndSend(exchangeName, secondHandSoldRoutingKey, event, correlationData);
    }

    private void publishDonationDecided(DonationRequestDecidedEvent domainEvent) {
        String nuevoEstado = domainEvent.approved() ? "APROBADA" : "RECHAZADA";
        SegundaVidaDonacionEstadoCambiadoIntegrationEvent event = SegundaVidaDonacionEstadoCambiadoIntegrationEvent.builder()
                .solicitudId(domainEvent.requestId())
                .requestId(domainEvent.requestId())
                .fundacionId(domainEvent.foundationId())
                .foundationId(domainEvent.foundationId())
                .estadoAnterior("PENDIENTE")
                .nuevoEstado(nuevoEstado)
                .estado(nuevoEstado)
                .status(nuevoEstado)
                .motivo(domainEvent.rejectionReason())
                .mensaje(domainEvent.rejectionReason())
                .build();
        CorrelationData correlationData = new CorrelationData(event.getEventoId());

        log.info(">>> [RabbitMQ PUBLISH] Publicando decisión donación '{}' desde ms-segunda-vida-donaciones hacia Exchange '{}' con Routing Key '{}'",
                event.getTipoEvento(), exchangeName, donationStatusUpdatedRoutingKey);

        rabbitTemplate.convertAndSend(exchangeName, donationStatusUpdatedRoutingKey, event, correlationData);
    }

    private void publishPickupScheduled(PickupScheduledEvent domainEvent) {
        SegundaVidaDonacionEstadoCambiadoIntegrationEvent event = SegundaVidaDonacionEstadoCambiadoIntegrationEvent.builder()
                .solicitudId(domainEvent.requestId())
                .requestId(domainEvent.requestId())
                .estadoAnterior("ACEPTADA")
                .nuevoEstado("EN_RECOGIDA")
                .estado("EN_RECOGIDA")
                .status("SCHEDULED")
                .puntoEntrega(domainEvent.deliveryPoint())
                .deliveryPoint(domainEvent.deliveryPoint())
                .fechaProgramada(domainEvent.startTime() != null ? domainEvent.startTime().toString() : null)
                .mensaje("Recogida programada en: " + domainEvent.deliveryPoint())
                .build();
        CorrelationData correlationData = new CorrelationData(event.getEventoId());

        rabbitTemplate.convertAndSend(exchangeName, donationStatusUpdatedRoutingKey, event, correlationData);
    }

    private void publishDonationDelivered(DonationDeliveredEvent domainEvent) {
        SegundaVidaDonacionEstadoCambiadoIntegrationEvent event = SegundaVidaDonacionEstadoCambiadoIntegrationEvent.builder()
                .solicitudId(domainEvent.requestId())
                .requestId(domainEvent.requestId())
                .publicacionId(domainEvent.publicationId())
                .publicationId(domainEvent.publicationId())
                .fundacionId(domainEvent.foundationId())
                .foundationId(domainEvent.foundationId())
                .estadoAnterior("EN_RECOGIDA")
                .nuevoEstado("ENTREGADA")
                .estado("ENTREGADA")
                .status("COMPLETED")
                .build();
        CorrelationData correlationData = new CorrelationData(event.getEventoId());

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

    public String getSecondHandSoldRoutingKey() {
        return secondHandSoldRoutingKey;
    }
}
