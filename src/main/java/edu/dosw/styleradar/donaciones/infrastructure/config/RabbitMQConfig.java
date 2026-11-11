package edu.dosw.styleradar.donaciones.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Configuración de infraestructura RabbitMQ para el broker AMQP (puerto estándar 5672).
 * Declara el TopicExchange 'donaciones.exchange', las colas de integración para ms-notificaciones,
 * confirmaciones de publicación (Publisher Confirms / Returns) y la serialización JSON.
 */
@Configuration
public class RabbitMQConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfig.class);

    public static final String DONATIONS_EXCHANGE_DEFAULT = "donaciones.exchange";
    public static final String DONATION_REQUEST_CREATED_ROUTING_KEY_DEFAULT = "donacion.solicitud.creada";
    public static final String DONATION_STATUS_UPDATED_ROUTING_KEY_DEFAULT = "donacion.estado.actualizado";
    public static final String NOTIFICATIONS_QUEUE_DEFAULT = "q.notificaciones.donacion-solicitud-creada";
    public static final String SIMULATOR_QUEUE_DEFAULT = "q.donaciones.simulador-notificaciones";

    @Bean
    public TopicExchange donationsTopicExchange(
            @Value("${app.rabbitmq.exchange.donations:" + DONATIONS_EXCHANGE_DEFAULT + "}") String exchangeName) {
        return ExchangeBuilder.topicExchange(exchangeName)
                .durable(true)
                .build();
    }

    /**
     * Cola oficial destinada al microservicio real de notificaciones (ms-notificaciones en :8300).
     */
    @Bean
    public Queue notificationsDonationCreatedQueue(
            @Value("${app.rabbitmq.queues.notifications-donation-created:" + NOTIFICATIONS_QUEUE_DEFAULT + "}") String queueName) {
        return QueueBuilder.durable(queueName)
                .build();
    }

    @Bean
    public Binding notificationsDonationCreatedBinding(
            Queue notificationsDonationCreatedQueue,
            TopicExchange donationsTopicExchange,
            @Value("${app.rabbitmq.routing-keys.donation-request-created:" + DONATION_REQUEST_CREATED_ROUTING_KEY_DEFAULT + "}") String routingKey) {
        return BindingBuilder.bind(notificationsDonationCreatedQueue)
                .to(donationsTopicExchange)
                .with(routingKey);
    }

    /**
     * Cola independiente para el simulador de diagnóstico. Solo se declara si el simulador está explícitamente habilitado.
     */
    @Bean
    @ConditionalOnProperty(name = "app.rabbitmq.simulator.enabled", havingValue = "true", matchIfMissing = false)
    public Queue simulatorNotificationsQueue(
            @Value("${app.rabbitmq.queues.simulator-queue:" + SIMULATOR_QUEUE_DEFAULT + "}") String queueName) {
        return QueueBuilder.durable(queueName)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.rabbitmq.simulator.enabled", havingValue = "true", matchIfMissing = false)
    public Binding simulatorNotificationsBinding(
            Queue simulatorNotificationsQueue,
            TopicExchange donationsTopicExchange,
            @Value("${app.rabbitmq.routing-keys.donation-request-created:" + DONATION_REQUEST_CREATED_ROUTING_KEY_DEFAULT + "}") String routingKey) {
        return BindingBuilder.bind(simulatorNotificationsQueue)
                .to(donationsTopicExchange)
                .with(routingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jackson2JsonMessageConverter);
        template.setMandatory(true);
        template.setConfirmCallback(confirmCallback());
        template.setReturnsCallback(returnsCallback());
        return template;
    }

    public RabbitTemplate.ConfirmCallback confirmCallback() {
        return (correlationData, ack, cause) -> {
            String eventId = correlationData != null ? correlationData.getId() : "desconocido";
            if (ack) {
                log.info("RabbitMQ broker confirmó entrega (ACK) para el evento [EventId: {}]", eventId);
            } else {
                log.error("RabbitMQ broker NO confirmó entrega (NACK) para el evento [EventId: {}]. Causa: {}", eventId, cause);
            }
        };
    }

    public RabbitTemplate.ReturnsCallback returnsCallback() {
        return returned -> log.error(
                "Mensaje devuelto por el broker RabbitMQ (no enrutable) [ReplyCode: {}, ReplyText: {}, Exchange: {}, RoutingKey: {}]",
                returned.getReplyCode(), returned.getReplyText(), returned.getExchange(), returned.getRoutingKey());
    }
}
