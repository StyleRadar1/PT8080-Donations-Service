package edu.dosw.styleradar.donaciones.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class RabbitMQConfigTest {

    private final RabbitMQConfig rabbitMQConfig = new RabbitMQConfig();

    @Test
    @DisplayName("Debe crear TopicExchange durable styleradar.exchange con configuración correcta")
    void shouldCreateDonationsTopicExchange() {
        TopicExchange exchange = rabbitMQConfig.donationsTopicExchange(RabbitMQConfig.STYLERADAR_EXCHANGE_DEFAULT);

        assertThat(exchange).isNotNull();
        assertThat(exchange.getName()).isEqualTo("styleradar.exchange");
        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.isAutoDelete()).isFalse();
    }

    @Test
    @DisplayName("Debe crear DirectExchange durable styleradar.dlx y cola styleradar.donaciones.dlq")
    void shouldCreateDeadLetterExchangeAndQueue() {
        DirectExchange dlx = rabbitMQConfig.deadLetterExchange(RabbitMQConfig.STYLERADAR_DLX_DEFAULT);
        assertThat(dlx).isNotNull();
        assertThat(dlx.getName()).isEqualTo("styleradar.dlx");
        assertThat(dlx.isDurable()).isTrue();

        Queue dlq = rabbitMQConfig.deadLetterQueue(RabbitMQConfig.DONACIONES_DLQ_DEFAULT);
        assertThat(dlq).isNotNull();
        assertThat(dlq.getName()).isEqualTo("styleradar.donaciones.dlq");
        assertThat(dlq.isDurable()).isTrue();

        Binding binding = rabbitMQConfig.deadLetterBinding(dlq, dlx);
        assertThat(binding).isNotNull();
        assertThat(binding.getDestination()).isEqualTo("styleradar.donaciones.dlq");
        assertThat(binding.getExchange()).isEqualTo("styleradar.dlx");
    }

    @Test
    @DisplayName("Debe crear cola durable q.notificaciones.donacion-solicitud-creada para el microservicio real")
    void shouldCreateNotificationsQueue() {
        Queue queue = rabbitMQConfig.notificationsDonationCreatedQueue(RabbitMQConfig.NOTIFICATIONS_QUEUE_DEFAULT);

        assertThat(queue).isNotNull();
        assertThat(queue.getName()).isEqualTo("q.notificaciones.donacion-solicitud-creada");
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    @DisplayName("Debe crear binding entre la cola de notificaciones y styleradar.exchange con routing key esperada")
    void shouldCreateNotificationsBinding() {
        TopicExchange exchange = rabbitMQConfig.donationsTopicExchange("styleradar.exchange");
        Queue queue = rabbitMQConfig.notificationsDonationCreatedQueue("q.notificaciones.donacion-solicitud-creada");

        Binding binding = rabbitMQConfig.notificationsDonationCreatedBinding(
                queue,
                exchange,
                "styleradar.donaciones.estado.cambiado");

        assertThat(binding).isNotNull();
        assertThat(binding.getDestination()).isEqualTo("q.notificaciones.donacion-solicitud-creada");
        assertThat(binding.getExchange()).isEqualTo("styleradar.exchange");
        assertThat(binding.getRoutingKey()).isEqualTo("styleradar.donaciones.estado.cambiado");
        assertThat(binding.getDestinationType()).isEqualTo(Binding.DestinationType.QUEUE);
    }

    @Test
    @DisplayName("Debe crear cola independiente para el simulador sin interferir con la cola de producción")
    void shouldCreateSimulatorQueueAndBinding() {
        Queue queue = rabbitMQConfig.simulatorNotificationsQueue(RabbitMQConfig.SIMULATOR_QUEUE_DEFAULT);
        assertThat(queue).isNotNull();
        assertThat(queue.getName()).isEqualTo("q.donaciones.simulador-notificaciones");

        TopicExchange exchange = rabbitMQConfig.donationsTopicExchange("styleradar.exchange");
        Binding binding = rabbitMQConfig.simulatorNotificationsBinding(queue, exchange, "styleradar.donaciones.estado.cambiado");

        assertThat(binding).isNotNull();
        assertThat(binding.getDestination()).isEqualTo("q.donaciones.simulador-notificaciones");
        assertThat(binding.getRoutingKey()).isEqualTo("styleradar.donaciones.estado.cambiado");
    }

    @Test
    @DisplayName("Debe configurar Jackson2JsonMessageConverter con ObjectMapper sin cabecera TypeId restrictiva")
    void shouldCreateJackson2JsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        Jackson2JsonMessageConverter converter = rabbitMQConfig.jackson2JsonMessageConverter(objectMapper);

        assertThat(converter).isNotNull();
    }

    @Test
    @DisplayName("Debe configurar RabbitTemplate con confirms (ACK/NACK), returns y mandatory")
    void shouldConfigureRabbitTemplateWithConfirmsAndReturns() {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(new ObjectMapper());

        RabbitTemplate rabbitTemplate = rabbitMQConfig.rabbitTemplate(connectionFactory, converter);

        assertThat(rabbitTemplate).isNotNull();
        assertThat(rabbitTemplate.getConnectionFactory()).isSameAs(connectionFactory);
        assertThat(rabbitTemplate.getMessageConverter()).isSameAs(converter);

        assertDoesNotThrow(() -> {
            CorrelationData correlationData = new CorrelationData("event-ack-1");
            rabbitMQConfig.confirmCallback().confirm(correlationData, true, null);
            rabbitMQConfig.confirmCallback().confirm(correlationData, false, "Queue full");
            rabbitMQConfig.confirmCallback().confirm(null, false, "Unknown error");

            ReturnedMessage returned = new ReturnedMessage(
                    new Message("{}".getBytes()),
                    312,
                    "NO_ROUTE",
                    "styleradar.exchange",
                    "styleradar.donaciones.estado.cambiado");
            rabbitMQConfig.returnsCallback().returnedMessage(returned);
        });
    }
}
