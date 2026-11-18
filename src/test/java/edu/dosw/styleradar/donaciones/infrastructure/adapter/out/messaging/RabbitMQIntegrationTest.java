package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.RabbitMQContainer;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.dosw.styleradar.donaciones.domain.event.DonationRequestCreatedEvent;
import edu.dosw.styleradar.donaciones.domain.event.DonationRequestDecidedEvent;
import edu.dosw.styleradar.donaciones.domain.event.PublicationSoldEvent;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging.event.DonationRequestCreatedIntegrationEvent;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging.event.SegundaVidaDonacionEstadoCambiadoIntegrationEvent;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging.event.SegundaVidaDonacionPrendaVendidaIntegrationEvent;
import edu.dosw.styleradar.donaciones.infrastructure.config.RabbitMQConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de integración con Testcontainers RabbitMQ para validar los contratos reales
 * de publicación y consumo sobre 'styleradar.exchange' hacia 'ms-notificaciones' y 'ms-chat'.
 */
class RabbitMQIntegrationTest {

    static RabbitMQContainer rabbitMQContainer;
    static CachingConnectionFactory connectionFactory;
    static RabbitTemplate rabbitTemplate;
    static RabbitAdmin rabbitAdmin;

    private static final String EXCHANGE_NAME = "styleradar.exchange";
    private static final String ROUTING_KEY_CREATED = "styleradar.donaciones.solicitud.creada";
    private static final String ROUTING_KEY_STATUS = "styleradar.donaciones.estado.cambiado";
    private static final String ROUTING_KEY_SOLD_CHAT = "segunda-mano.prenda.vendida";
    private static final String ROUTING_KEY_SOLD_NOTIF = "styleradar.segundavida.prenda.vendida";

    private static final String QUEUE_CREATED = "q.notificaciones.donacion-solicitud-creada";
    private static final String QUEUE_STATUS = "q.notificaciones.donacion-estado-cambiado";
    private static final String QUEUE_SOLD_CHAT = "q.chat.segunda-mano";
    private static final String QUEUE_SOLD_NOTIF = "q.notificaciones.prenda-vendida";

    @BeforeAll
    static void setUp() {
        System.setProperty("api.version", "1.44");
        boolean isCi = "true".equalsIgnoreCase(System.getenv("CI"))
                || "true".equalsIgnoreCase(System.getenv("GITHUB_ACTIONS"));
        boolean dockerAvailable = DockerClientFactory.instance().isDockerAvailable();

        if (!isCi && !dockerAvailable) {
            Assumptions.abort("Docker no está disponible en el entorno local. Se omite la prueba de integración con RabbitMQ.");
            return;
        }

        rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.12-management-alpine");
        rabbitMQContainer.start();

        connectionFactory = new CachingConnectionFactory(rabbitMQContainer.getHost(), rabbitMQContainer.getAmqpPort());
        connectionFactory.setUsername(rabbitMQContainer.getAdminUsername());
        connectionFactory.setPassword(rabbitMQContainer.getAdminPassword());

        Jackson2JsonMessageConverter converter = new RabbitMQConfig().jackson2JsonMessageConverter(new ObjectMapper());

        rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter);

        rabbitAdmin = new RabbitAdmin(connectionFactory);

        TopicExchange exchange = ExchangeBuilder.topicExchange(EXCHANGE_NAME).durable(true).build();
        rabbitAdmin.declareExchange(exchange);

        declareQueueAndBinding(exchange, QUEUE_CREATED, ROUTING_KEY_CREATED);
        declareQueueAndBinding(exchange, QUEUE_STATUS, ROUTING_KEY_STATUS);
        declareQueueAndBinding(exchange, QUEUE_SOLD_CHAT, ROUTING_KEY_SOLD_CHAT);
        declareQueueAndBinding(exchange, QUEUE_SOLD_NOTIF, ROUTING_KEY_SOLD_NOTIF);
    }

    private static void declareQueueAndBinding(TopicExchange exchange, String queueName, String routingKey) {
        Queue queue = QueueBuilder.durable(queueName).build();
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(routingKey);
        rabbitAdmin.declareQueue(queue);
        rabbitAdmin.declareBinding(binding);
    }

    @AfterAll
    static void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
        if (rabbitMQContainer != null && rabbitMQContainer.isRunning()) {
            rabbitMQContainer.stop();
        }
    }

    @Test
    @DisplayName("Debe publicar y consumir eventos reales en styleradar.exchange para Chat y Notificaciones")
    void shouldPublishAndConsumeEventThroughRealRabbitMQBroker() {
        Assumptions.assumeTrue(rabbitMQContainer != null && rabbitMQContainer.isRunning(),
                "RabbitMQ Testcontainer no está corriendo.");

        DonationEventPublisherAdapter adapter = new DonationEventPublisherAdapter(
                rabbitTemplate,
                EXCHANGE_NAME,
                EXCHANGE_NAME,
                ROUTING_KEY_STATUS,
                ROUTING_KEY_SOLD_CHAT,
                ROUTING_KEY_SOLD_NOTIF,
                ROUTING_KEY_CREATED);

        // 1. Solicitud creada -> ms-notificaciones (con representanteId)
        adapter.publishEvent(new DonationRequestCreatedEvent(777L, 888L, 999L, 111L, "PENDING", 24L));
        DonationRequestCreatedIntegrationEvent createdEvent = rabbitTemplate.receiveAndConvert(
                QUEUE_CREATED, 5000, new ParameterizedTypeReference<>() {});
        assertThat(createdEvent).isNotNull();
        assertThat(createdEvent.getEventType()).isEqualTo("donacion.solicitud.creada");
        assertThat(createdEvent.getRequestId()).isEqualTo(777L);
        assertThat(createdEvent.getRepresentanteId()).isEqualTo(24L);
        assertThat(createdEvent.getFundacionUsuarioId()).isEqualTo(24L);
        assertThat(createdEvent.getData().getEstado()).isEqualTo("PENDIENTE");

        // 2. Estado cambiado -> ms-notificaciones
        adapter.publishEvent(new DonationRequestDecidedEvent(777L, 111L, 999L, true, null));
        SegundaVidaDonacionEstadoCambiadoIntegrationEvent statusEvent = rabbitTemplate.receiveAndConvert(
                QUEUE_STATUS, 5000, new ParameterizedTypeReference<>() {});
        assertThat(statusEvent).isNotNull();
        assertThat(statusEvent.getDonacionId()).isEqualTo(777L);
        assertThat(statusEvent.getNuevoEstado()).isEqualTo("APROBADA");

        // 3. Prenda vendida -> ms-chat (segunda-mano.prenda.vendida) y ms-notificaciones (styleradar.segundavida.prenda.vendida)
        adapter.publishEvent(new PublicationSoldEvent(50L, 101L, 102L, List.of(201L, 202L)));

        SegundaVidaDonacionPrendaVendidaIntegrationEvent chatSoldEvent = rabbitTemplate.receiveAndConvert(
                QUEUE_SOLD_CHAT, 5000, new ParameterizedTypeReference<>() {});
        assertThat(chatSoldEvent).isNotNull();
        assertThat(chatSoldEvent.getPublicacionId()).isEqualTo(50L);
        assertThat(chatSoldEvent.getCompradorId()).isEqualTo(102L);
        assertThat(chatSoldEvent.getInteresadosIds()).containsExactly(201L, 202L);

        SegundaVidaDonacionPrendaVendidaIntegrationEvent notifSoldEvent = rabbitTemplate.receiveAndConvert(
                QUEUE_SOLD_NOTIF, 5000, new ParameterizedTypeReference<>() {});
        assertThat(notifSoldEvent).isNotNull();
        assertThat(notifSoldEvent.getPublicacionId()).isEqualTo(50L);
        assertThat(notifSoldEvent.getVendedorId()).isEqualTo(101L);
        assertThat(notifSoldEvent.getCompradorId()).isEqualTo(102L);
        assertThat(notifSoldEvent.getInteresadosIds()).containsExactly(201L, 202L);
    }
}
