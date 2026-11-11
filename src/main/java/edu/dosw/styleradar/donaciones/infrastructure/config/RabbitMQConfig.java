package edu.dosw.styleradar.donaciones.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Configuración de infraestructura RabbitMQ para el broker AMQP central de StyleRadar (puerto 5671 TLS / 5672).
 * Declara el TopicExchange 'styleradar.exchange', el Dead Letter Exchange 'styleradar.dlx',
 * la cola de errores 'styleradar.donaciones.dlq', confirmaciones de publicación (Publisher Confirms / Returns)
 * y la serialización JSON estándar sin dependencias forzadas de cabeceras de tipo.
 */
@Configuration
public class RabbitMQConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfig.class);

    public static final String STYLERADAR_EXCHANGE_DEFAULT = "styleradar.exchange";
    public static final String STYLERADAR_DLX_DEFAULT = "styleradar.dlx";
    public static final String DONACIONES_DLQ_DEFAULT = "styleradar.donaciones.dlq";
    public static final String DONACION_ESTADO_CAMBIADO_ROUTING_KEY_DEFAULT = "styleradar.donaciones.estado.cambiado";
    public static final String SEGUNDA_VIDA_PRENDA_VENDIDA_ROUTING_KEY_DEFAULT = "styleradar.segundavida.prenda.vendida";

    // Retrocompatibilidad con nombres previos de Sprint 2
    public static final String DONATIONS_EXCHANGE_DEFAULT = STYLERADAR_EXCHANGE_DEFAULT;
    public static final String DONATION_REQUEST_CREATED_ROUTING_KEY_DEFAULT = DONACION_ESTADO_CAMBIADO_ROUTING_KEY_DEFAULT;
    public static final String DONATION_STATUS_UPDATED_ROUTING_KEY_DEFAULT = DONACION_ESTADO_CAMBIADO_ROUTING_KEY_DEFAULT;
    public static final String NOTIFICATIONS_QUEUE_DEFAULT = "q.notificaciones.donacion-solicitud-creada";
    public static final String SIMULATOR_QUEUE_DEFAULT = "q.donaciones.simulador-notificaciones";

    @Bean
    public TopicExchange donationsTopicExchange(
            @Value("${app.rabbitmq.exchange.main:${app.rabbitmq.exchange.donations:" + STYLERADAR_EXCHANGE_DEFAULT + "}}") String exchangeName) {
        return ExchangeBuilder.topicExchange(exchangeName)
                .durable(true)
                .build();
    }

    @Bean
    public DirectExchange deadLetterExchange(
            @Value("${app.rabbitmq.exchange.dlx:" + STYLERADAR_DLX_DEFAULT + "}") String dlxName) {
        return ExchangeBuilder.directExchange(dlxName)
                .durable(true)
                .build();
    }

    @Bean
    public Queue deadLetterQueue(
            @Value("${app.rabbitmq.queues.dlq:" + DONACIONES_DLQ_DEFAULT + "}") String dlqName) {
        return QueueBuilder.durable(dlqName)
                .build();
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(deadLetterQueue.getName());
    }

    /**
     * Cola oficial de integración para notificaciones (suscriptora de eventos de donación).
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
            @Value("${app.rabbitmq.routing-keys.donation-status-updated:" + DONACION_ESTADO_CAMBIADO_ROUTING_KEY_DEFAULT + "}") String routingKey) {
        return BindingBuilder.bind(notificationsDonationCreatedQueue)
                .to(donationsTopicExchange)
                .with(routingKey);
    }

    /**
     * Cola independiente para el simulador de diagnóstico en ambientes de desarrollo/pruebas.
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
            @Value("${app.rabbitmq.routing-keys.donation-status-updated:" + DONACION_ESTADO_CAMBIADO_ROUTING_KEY_DEFAULT + "}") String routingKey) {
        return BindingBuilder.bind(simulatorNotificationsQueue)
                .to(donationsTopicExchange)
                .with(routingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultClassMapper classMapper = new DefaultClassMapper();
        classMapper.setTrustedPackages("*");
        converter.setClassMapper(classMapper);
        converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
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
                log.info("RabbitMQ broker confirmó entrega (ACK) en 'styleradar.exchange' [EventId: {}]", eventId);
            } else {
                log.error("RabbitMQ broker NO confirmó entrega (NACK) [EventId: {}]. Causa: {}", eventId, cause);
            }
        };
    }

    public RabbitTemplate.ReturnsCallback returnsCallback() {
        return returned -> {
            log.error("Mensaje devuelto por el broker RabbitMQ (no enrutable) [ReplyCode: {}, ReplyText: {}, Exchange: {}, RoutingKey: {}]",
                    returned.getReplyCode(), returned.getReplyText(), returned.getExchange(), returned.getRoutingKey());
        };
    }
}
