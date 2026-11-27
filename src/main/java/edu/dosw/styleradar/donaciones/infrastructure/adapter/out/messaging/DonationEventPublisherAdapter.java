package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import edu.dosw.styleradar.donaciones.domain.event.DonationDeliveredEvent;
import edu.dosw.styleradar.donaciones.domain.event.DonationDeliveredStatisticsEvent;
import edu.dosw.styleradar.donaciones.domain.event.DonationRequestCreatedEvent;
import edu.dosw.styleradar.donaciones.domain.event.DonationRequestDecidedEvent;
import edu.dosw.styleradar.donaciones.domain.event.PickupScheduledEvent;
import edu.dosw.styleradar.donaciones.domain.event.PublicationSoldEvent;
import edu.dosw.styleradar.donaciones.domain.exception.ExternalServiceUnavailableException;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationEventPublisherPort;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging.event.DonationCompletedStatisticsIntegrationEvent;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging.event.DonationRequestCreatedIntegrationEvent;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging.event.IntegrationDateFormatter;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging.event.SegundaVidaDonacionEstadoCambiadoIntegrationEvent;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging.event.SegundaVidaDonacionPrendaVendidaIntegrationEvent;
import edu.dosw.styleradar.donaciones.infrastructure.config.RabbitMQConfig;

/**
 * Adaptador de salida RabbitMQ para la publicación asíncrona hacia 'styleradar.exchange' (puerto 5671 TLS / 5672).
 * Flujos de 'ms-segunda-vida-donaciones':
 * 1) 'styleradar.donaciones.solicitud.creada' y 'styleradar.donaciones.estado.cambiado' hacia ms-notificaciones.
 * 2) 'styleradar.donaciones.completada' hacia ms-estadisticas cuando se confirma recepción de donación.
 * 3) 'segunda-mano.prenda.vendida' hacia ms-chat para cierre de salas.
 * 4) 'styleradar.segundavida.prenda.vendida' hacia ms-notificaciones para alertas de venta C2C.
 */
@Component("donacionEventPublisherAdapter")
public class DonationEventPublisherAdapter implements DonationEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(DonationEventPublisherAdapter.class);

    private static final String STATUS_IN_PICKUP = "CITA_PROGRAMADA";
    private static final String STATUS_DELIVERED = "ENTREGADA";
    private static final String DEFAULT_CHAT_SOLD_KEY = "segunda-mano.prenda.vendida";
    private static final String DEFAULT_NOTIFICATIONS_SOLD_KEY = "styleradar.segundavida.prenda.vendida";
    private static final String DEFAULT_DONATION_COMPLETED_KEY = RabbitMQConfig.DONACION_COMPLETADA_ROUTING_KEY_DEFAULT;
    private static final long DEFAULT_CONFIRM_TIMEOUT_MS = 5000L;

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeName;
    private final String chatExchangeName;
    private final String donationStatusUpdatedRoutingKey;
    private final String donationCompletedRoutingKey;
    private final String secondHandSoldRoutingKey;
    private final String notificationsSecondHandSoldRoutingKey;
    private final String donationRequestCreatedRoutingKey;
    private long confirmTimeoutMs = DEFAULT_CONFIRM_TIMEOUT_MS;

    @Autowired
    public DonationEventPublisherAdapter(
            RabbitTemplate rabbitTemplate,
            @Value("${app.rabbitmq.exchange.main:${app.rabbitmq.exchange.donations:styleradar.exchange}}") String exchangeName,
            @Value("${app.rabbitmq.exchange.chat:styleradar.exchange}") String chatExchangeName,
            @Value("${app.rabbitmq.routing-keys.donation-status-updated:styleradar.donaciones.estado.cambiado}") String donationStatusUpdatedRoutingKey,
            @Value("${app.rabbitmq.routing-keys.donation-completed:" + DEFAULT_DONATION_COMPLETED_KEY + "}") String donationCompletedRoutingKey,
            @Value("${app.rabbitmq.routing-keys.second-hand-sold:segunda-mano.prenda.vendida}") String secondHandSoldRoutingKey,
            @Value("${app.rabbitmq.routing-keys.notifications-second-hand-sold:styleradar.segundavida.prenda.vendida}") String notificationsSecondHandSoldRoutingKey,
            @Value("${app.rabbitmq.routing-keys.donation-request-created:styleradar.donaciones.solicitud.creada}") String donationRequestCreatedRoutingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeName = exchangeName;
        this.chatExchangeName = chatExchangeName;
        this.donationStatusUpdatedRoutingKey = donationStatusUpdatedRoutingKey;
        this.donationCompletedRoutingKey = donationCompletedRoutingKey;
        this.secondHandSoldRoutingKey = secondHandSoldRoutingKey;
        this.notificationsSecondHandSoldRoutingKey = notificationsSecondHandSoldRoutingKey;
        this.donationRequestCreatedRoutingKey = donationRequestCreatedRoutingKey;
    }

    public DonationEventPublisherAdapter(
            RabbitTemplate rabbitTemplate,
            String exchangeName,
            String chatExchangeName,
            String donationStatusUpdatedRoutingKey,
            String secondHandSoldRoutingKey,
            String notificationsSecondHandSoldRoutingKey,
            String donationRequestCreatedRoutingKey) {
        this(rabbitTemplate, exchangeName, chatExchangeName, donationStatusUpdatedRoutingKey,
                DEFAULT_DONATION_COMPLETED_KEY, secondHandSoldRoutingKey,
                notificationsSecondHandSoldRoutingKey, donationRequestCreatedRoutingKey);
    }

    public DonationEventPublisherAdapter(
            RabbitTemplate rabbitTemplate,
            String exchangeName,
            String chatExchangeName,
            String donationStatusUpdatedRoutingKey,
            String secondHandSoldRoutingKey,
            String donationRequestCreatedRoutingKey) {
        this(rabbitTemplate, exchangeName, chatExchangeName, donationStatusUpdatedRoutingKey,
                DEFAULT_DONATION_COMPLETED_KEY, secondHandSoldRoutingKey,
                DEFAULT_NOTIFICATIONS_SOLD_KEY, donationRequestCreatedRoutingKey);
    }

    public DonationEventPublisherAdapter(
            RabbitTemplate rabbitTemplate,
            String exchangeName,
            String donationStatusUpdatedRoutingKey,
            String secondHandSoldRoutingKey,
            String donationRequestCreatedRoutingKey) {
        this(rabbitTemplate, exchangeName, "styleradar.exchange", donationStatusUpdatedRoutingKey,
                DEFAULT_DONATION_COMPLETED_KEY, secondHandSoldRoutingKey,
                DEFAULT_NOTIFICATIONS_SOLD_KEY, donationRequestCreatedRoutingKey);
    }

    public DonationEventPublisherAdapter(
            RabbitTemplate rabbitTemplate,
            String exchangeName,
            String donationRequestCreatedRoutingKey,
            String donationStatusUpdatedRoutingKey) {
        this(rabbitTemplate, exchangeName, "styleradar.exchange", donationStatusUpdatedRoutingKey,
                DEFAULT_DONATION_COMPLETED_KEY, DEFAULT_CHAT_SOLD_KEY,
                DEFAULT_NOTIFICATIONS_SOLD_KEY, donationRequestCreatedRoutingKey);
    }

    @Override
    public void publishEvent(Object event) {
        publishEventInternal(event);
    }

    @Override
    public void publishEventConfirmed(Object event) {
        CorrelationData correlationData = publishEventInternal(event);
        if (correlationData == null) {
            return;
        }
        awaitBrokerConfirmation(correlationData);
    }

    private CorrelationData publishEventInternal(Object event) {
        if (event == null) {
            log.warn("Se omitió publicación de evento nulo hacia RabbitMQ.");
            return null;
        }

        return switch (event) {
            case DonationRequestCreatedEvent domainEvent -> publishDonationRequestCreated(domainEvent);
            case PublicationSoldEvent soldEvent -> publishPublicationSold(soldEvent);
            case DonationRequestDecidedEvent decidedEvent -> publishDonationDecided(decidedEvent);
            case PickupScheduledEvent pickupEvent -> publishPickupScheduled(pickupEvent);
            case DonationDeliveredEvent deliveredEvent -> publishDonationDelivered(deliveredEvent);
            case DonationDeliveredStatisticsEvent statsEvent -> publishDonationDeliveredStatistics(statsEvent);
            default -> {
                log.info("Publicando evento en broker RabbitMQ [Exchange: {}, Evento: {}]",
                        exchangeName, event.getClass().getSimpleName());
                CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
                rabbitTemplate.convertAndSend(exchangeName, donationStatusUpdatedRoutingKey, event, correlationData);
                yield correlationData;
            }
        };
    }

    private void awaitBrokerConfirmation(CorrelationData correlationData) {
        try {
            CorrelationData.Confirm confirm = correlationData.getFuture().get(confirmTimeoutMs, TimeUnit.MILLISECONDS);
            if (confirm == null || !confirm.isAck()) {
                String reason = confirm != null ? confirm.getReason() : "Confirmación nula";
                throw new ExternalServiceUnavailableException(
                        "El broker RabbitMQ rechazó (NACK) la publicación [EventId="
                                + correlationData.getId() + "]: " + reason);
            }
            var returned = correlationData.getReturned();
            if (returned != null) {
                throw new ExternalServiceUnavailableException(
                        "El mensaje fue devuelto por RabbitMQ como no enrutable [EventId="
                                + correlationData.getId() + ", ReplyText="
                                + returned.getReplyText() + "]");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceUnavailableException(
                    "Interrupción esperando confirmación ACK de RabbitMQ [EventId=" + correlationData.getId() + "]", ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new ExternalServiceUnavailableException(
                    "Error o timeout esperando confirmación ACK de RabbitMQ [EventId=" + correlationData.getId() + "]", ex);
        }
    }

    public void setConfirmTimeoutMs(long confirmTimeoutMs) {
        this.confirmTimeoutMs = Math.max(1L, confirmTimeoutMs);
    }

    private CorrelationData publishDonationRequestCreated(DonationRequestCreatedEvent domainEvent) {
        DonationRequestCreatedIntegrationEvent integrationEvent = DonationRequestCreatedIntegrationEvent.from(domainEvent);
        CorrelationData correlationData = new CorrelationData(integrationEvent.getEventId());

        log.info(">>> [RabbitMQ PUBLISH] Publicando '{}' desde ms-segunda-vida-donaciones hacia Exchange '{}' con Routing Key '{}'",
                integrationEvent.getEventType(), exchangeName, donationRequestCreatedRoutingKey);

        rabbitTemplate.convertAndSend(exchangeName, donationRequestCreatedRoutingKey, integrationEvent, correlationData);
        return correlationData;
    }

    private CorrelationData publishPublicationSold(PublicationSoldEvent domainEvent) {
        SegundaVidaDonacionPrendaVendidaIntegrationEvent event = SegundaVidaDonacionPrendaVendidaIntegrationEvent.from(domainEvent);
        CorrelationData correlationData = new CorrelationData(event.getCorrelationId());

        log.info(">>> [RabbitMQ PUBLISH] Publicando venta C2C '{}' desde ms-segunda-vida-donaciones hacia Exchange '{}' con Routing Key '{}' para ms-chat",
                event.getEventType(), chatExchangeName, secondHandSoldRoutingKey);

        rabbitTemplate.convertAndSend(chatExchangeName, secondHandSoldRoutingKey, event, correlationData);

        if (exchangeName != null && notificationsSecondHandSoldRoutingKey != null
                && !notificationsSecondHandSoldRoutingKey.isBlank()
                && (!exchangeName.equals(chatExchangeName)
                || !notificationsSecondHandSoldRoutingKey.equals(secondHandSoldRoutingKey))) {
            log.info(">>> [RabbitMQ PUBLISH] Publicando evento venta C2C hacia Exchange '{}' con Routing Key '{}' para ms-notificaciones",
                    exchangeName, notificationsSecondHandSoldRoutingKey);
            rabbitTemplate.convertAndSend(exchangeName, notificationsSecondHandSoldRoutingKey, event, correlationData);
        }
        return correlationData;
    }

    private CorrelationData publishDonationDecided(DonationRequestDecidedEvent domainEvent) {
        String newStatus = domainEvent.approved() ? "APROBADA" : "RECHAZADA";
        String fechaCambio = IntegrationDateFormatter.formatLocalDateTime(domainEvent.occurredOn());
        String effectiveEventId = (domainEvent.eventId() != null && !domainEvent.eventId().isBlank())
                ? domainEvent.eventId().trim()
                : UUID.randomUUID().toString();
        SegundaVidaDonacionEstadoCambiadoIntegrationEvent.EstadoCambiadoData data =
                SegundaVidaDonacionEstadoCambiadoIntegrationEvent.EstadoCambiadoData.builder()
                        .donacionId(domainEvent.requestId())
                        .solicitudId(domainEvent.requestId())
                        .requestId(domainEvent.requestId())
                        .donanteId(domainEvent.donorId())
                        .donorId(domainEvent.donorId())
                        .fundacionId(domainEvent.foundationId())
                        .foundationId(domainEvent.foundationId())
                        .estadoAnterior("PENDIENTE")
                        .nuevoEstado(newStatus)
                        .estado(newStatus)
                        .status(newStatus)
                        .motivo(domainEvent.rejectionReason())
                        .mensaje(domainEvent.rejectionReason())
                        .fechaCambio(fechaCambio)
                        .build();

        SegundaVidaDonacionEstadoCambiadoIntegrationEvent event = SegundaVidaDonacionEstadoCambiadoIntegrationEvent.builder()
                .eventoId(effectiveEventId)
                .eventId(effectiveEventId)
                .correlationId(effectiveEventId)
                .donacionId(domainEvent.requestId())
                .solicitudId(domainEvent.requestId())
                .requestId(domainEvent.requestId())
                .donanteId(domainEvent.donorId())
                .donorId(domainEvent.donorId())
                .fundacionId(domainEvent.foundationId())
                .foundationId(domainEvent.foundationId())
                .estadoAnterior("PENDIENTE")
                .nuevoEstado(newStatus)
                .estado(newStatus)
                .status(newStatus)
                .motivo(domainEvent.rejectionReason())
                .mensaje(domainEvent.rejectionReason())
                .fechaCambio(fechaCambio)
                .fechaEvento(fechaCambio)
                .data(data)
                .build();
        CorrelationData correlationData = new CorrelationData(effectiveEventId);

        log.info(">>> [RabbitMQ PUBLISH] Publicando decisión donación '{}' desde ms-segunda-vida-donaciones hacia Exchange '{}' con Routing Key '{}'",
                event.getTipoEvento(), exchangeName, donationStatusUpdatedRoutingKey);

        rabbitTemplate.convertAndSend(exchangeName, donationStatusUpdatedRoutingKey, event, correlationData);
        return correlationData;
    }

    private CorrelationData publishPickupScheduled(PickupScheduledEvent domainEvent) {
        String fechaCambio = IntegrationDateFormatter.formatLocalDateTime(domainEvent.occurredOn());
        String scheduledDate = IntegrationDateFormatter.formatLocalDateTime(domainEvent.startTime());
        String reason = domainEvent.deliveryPoint() != null && !domainEvent.deliveryPoint().isBlank()
                ? "Punto de entrega: " + domainEvent.deliveryPoint()
                : null;
        SegundaVidaDonacionEstadoCambiadoIntegrationEvent.EstadoCambiadoData data =
                SegundaVidaDonacionEstadoCambiadoIntegrationEvent.EstadoCambiadoData.builder()
                        .donacionId(domainEvent.requestId())
                        .solicitudId(domainEvent.requestId())
                        .requestId(domainEvent.requestId())
                        .donanteId(domainEvent.donorId())
                        .donorId(domainEvent.donorId())
                        .estadoAnterior("ACEPTADA")
                        .nuevoEstado(STATUS_IN_PICKUP)
                        .estado(STATUS_IN_PICKUP)
                        .status("SCHEDULED")
                        .puntoEntrega(domainEvent.deliveryPoint())
                        .deliveryPoint(domainEvent.deliveryPoint())
                        .fechaProgramada(scheduledDate)
                        .motivo(reason)
                        .mensaje(reason)
                        .fechaCambio(fechaCambio)
                        .build();

        SegundaVidaDonacionEstadoCambiadoIntegrationEvent event = SegundaVidaDonacionEstadoCambiadoIntegrationEvent.builder()
                .donacionId(domainEvent.requestId())
                .solicitudId(domainEvent.requestId())
                .requestId(domainEvent.requestId())
                .donanteId(domainEvent.donorId())
                .donorId(domainEvent.donorId())
                .estadoAnterior("ACEPTADA")
                .nuevoEstado(STATUS_IN_PICKUP)
                .estado(STATUS_IN_PICKUP)
                .status("SCHEDULED")
                .puntoEntrega(domainEvent.deliveryPoint())
                .deliveryPoint(domainEvent.deliveryPoint())
                .fechaProgramada(scheduledDate)
                .motivo(reason)
                .mensaje(reason)
                .fechaCambio(fechaCambio)
                .fechaEvento(fechaCambio)
                .data(data)
                .build();
        CorrelationData correlationData = new CorrelationData(event.getCorrelationId());

        rabbitTemplate.convertAndSend(exchangeName, donationStatusUpdatedRoutingKey, event, correlationData);
        return correlationData;
    }

    private CorrelationData publishDonationDelivered(DonationDeliveredEvent domainEvent) {
        String fechaCambio = IntegrationDateFormatter.formatLocalDateTime(domainEvent.occurredOn());
        String effectiveEventId = resolveEventId(domainEvent.eventId());
        SegundaVidaDonacionEstadoCambiadoIntegrationEvent.EstadoCambiadoData data =
                SegundaVidaDonacionEstadoCambiadoIntegrationEvent.EstadoCambiadoData.builder()
                        .donacionId(domainEvent.requestId())
                        .solicitudId(domainEvent.requestId())
                        .requestId(domainEvent.requestId())
                        .donanteId(domainEvent.donorId())
                        .donorId(domainEvent.donorId())
                        .publicacionId(domainEvent.publicationId())
                        .publicationId(domainEvent.publicationId())
                        .fundacionId(domainEvent.foundationId())
                        .foundationId(domainEvent.foundationId())
                        .estadoAnterior(STATUS_IN_PICKUP)
                        .nuevoEstado(STATUS_DELIVERED)
                        .estado(STATUS_DELIVERED)
                        .status("COMPLETED")
                        .fechaCambio(fechaCambio)
                        .build();

        SegundaVidaDonacionEstadoCambiadoIntegrationEvent event = SegundaVidaDonacionEstadoCambiadoIntegrationEvent.builder()
                .eventoId(effectiveEventId)
                .eventId(effectiveEventId)
                .correlationId(effectiveEventId)
                .donacionId(domainEvent.requestId())
                .solicitudId(domainEvent.requestId())
                .requestId(domainEvent.requestId())
                .donanteId(domainEvent.donorId())
                .donorId(domainEvent.donorId())
                .publicacionId(domainEvent.publicationId())
                .publicationId(domainEvent.publicationId())
                .fundacionId(domainEvent.foundationId())
                .foundationId(domainEvent.foundationId())
                .estadoAnterior(STATUS_IN_PICKUP)
                .nuevoEstado(STATUS_DELIVERED)
                .estado(STATUS_DELIVERED)
                .status("COMPLETED")
                .fechaCambio(fechaCambio)
                .fechaEvento(fechaCambio)
                .data(data)
                .build();
        CorrelationData correlationData = new CorrelationData(effectiveEventId);

        rabbitTemplate.convertAndSend(exchangeName, donationStatusUpdatedRoutingKey, event, correlationData);
        return correlationData;
    }

    private CorrelationData publishDonationDeliveredStatistics(DonationDeliveredStatisticsEvent domainEvent) {
        if (exchangeName == null || donationCompletedRoutingKey == null || donationCompletedRoutingKey.isBlank()) {
            return null;
        }
        if (domainEvent.foundationId() == null) {
            log.warn("Se omitió publicación estadística '{}' para solicitud ID={} por falta de foundationId.",
                    donationCompletedRoutingKey, domainEvent.requestId());
            return null;
        }
        DonationCompletedStatisticsIntegrationEvent statsEvent =
                DonationCompletedStatisticsIntegrationEvent.from(domainEvent);
        CorrelationData statsCorrelation = new CorrelationData(statsEvent.getEventId());

        log.info(">>> [RabbitMQ PUBLISH] Publicando '{}' [EventId={}] desde ms-segunda-vida-donaciones hacia Exchange '{}' con Routing Key '{}' para ms-estadisticas",
                statsEvent.getEventType(), statsEvent.getEventId(), exchangeName, donationCompletedRoutingKey);

        rabbitTemplate.convertAndSend(exchangeName, donationCompletedRoutingKey, statsEvent, statsCorrelation);
        return statsCorrelation;
    }

    private static String resolveEventId(String explicitEventId) {
        return explicitEventId != null && !explicitEventId.isBlank()
                ? explicitEventId.trim()
                : UUID.randomUUID().toString();
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public String getChatExchangeName() {
        return chatExchangeName;
    }

    public String getDonationRequestCreatedRoutingKey() {
        return donationRequestCreatedRoutingKey;
    }

    public String getDonationStatusUpdatedRoutingKey() {
        return donationStatusUpdatedRoutingKey;
    }

    public String getDonationCompletedRoutingKey() {
        return donationCompletedRoutingKey;
    }

    public String getSecondHandSoldRoutingKey() {
        return secondHandSoldRoutingKey;
    }

    public String getNotificationsSecondHandSoldRoutingKey() {
        return notificationsSecondHandSoldRoutingKey;
    }
}
