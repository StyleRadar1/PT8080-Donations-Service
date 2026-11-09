package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging;

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
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.RabbitMQContainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.dosw.styleradar.donaciones.domain.event.DonationRequestCreatedEvent;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging.event.DonationRequestCreatedIntegrationEvent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de integración con Testcontainers RabbitMQ para validar la publicación
 * hacia 'donaciones.exchange' y su recepción asíncrona mediante la cola de 'ms-notificaciones'.
 */
class RabbitMQIntegrationTest {

    static RabbitMQContainer rabbitMQContainer;
    static CachingConnectionFactory connectionFactory;
    static RabbitTemplate rabbitTemplate;
    static RabbitAdmin rabbitAdmin;

    private static final String EXCHANGE_NAME = "donaciones.exchange";
    private static final String ROUTING_KEY = "donacion.solicitud.creada";
    private static final String QUEUE_NAME = "q.notificaciones.donacion-solicitud-creada";

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

        ObjectMapper objectMapper = new ObjectMapper();
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);

        rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter);

        rabbitAdmin = new RabbitAdmin(connectionFactory);

        TopicExchange exchange = ExchangeBuilder.topicExchange(EXCHANGE_NAME).durable(true).build();
        Queue queue = QueueBuilder.durable(QUEUE_NAME).build();
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);

        rabbitAdmin.declareExchange(exchange);
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
    @DisplayName("Debe publicar evento a donaciones.exchange y ser consumido desde la cola de ms-notificaciones con payload JSON intacto")
    void shouldPublishAndConsumeEventThroughRealRabbitMQBroker() {
        Assumptions.assumeTrue(rabbitMQContainer != null && rabbitMQContainer.isRunning(),
                "RabbitMQ Testcontainer no está corriendo.");

        DonationEventPublisherAdapter adapter = new DonationEventPublisherAdapter(
                rabbitTemplate,
                EXCHANGE_NAME,
                ROUTING_KEY,
                "donacion.estado.actualizado");

        DonationRequestCreatedEvent domainEvent = new DonationRequestCreatedEvent(777L, 888L, 999L, 111L);

        adapter.publishEvent(domainEvent);

        Object receivedMessage = rabbitTemplate.receiveAndConvert(QUEUE_NAME, 5000);
        assertThat(receivedMessage).isNotNull().isInstanceOf(DonationRequestCreatedIntegrationEvent.class);

        DonationRequestCreatedIntegrationEvent event = (DonationRequestCreatedIntegrationEvent) receivedMessage;
        assertThat(event.getEventType()).isEqualTo("donacion.solicitud.creada");
        assertThat(event.getRequestId()).isEqualTo(777L);
        assertThat(event.getPublicationId()).isEqualTo(888L);
        assertThat(event.getDonorId()).isEqualTo(999L);
        assertThat(event.getFoundationId()).isEqualTo(111L);
        assertThat(event.getData().getSolicitudId()).isEqualTo(777L);
        assertThat(event.getData().getEstado()).isEqualTo("PENDIENTE");
    }
}
