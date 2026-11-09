package edu.dosw.styleradar.donaciones.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.Binding;
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
    @DisplayName("Debe crear TopicExchange durable donaciones.exchange con configuración correcta")
    void shouldCreateDonationsTopicExchange() {
        TopicExchange exchange = rabbitMQConfig.donationsTopicExchange(RabbitMQConfig.DONATIONS_EXCHANGE_DEFAULT);

        assertThat(exchange).isNotNull();
        assertThat(exchange.getName()).isEqualTo("donaciones.exchange");
        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.isAutoDelete()).isFalse();
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
    @DisplayName("Debe crear binding entre la cola de notificaciones y donaciones.exchange con routing key esperada")
    void shouldCreateNotificationsBinding() {
        TopicExchange exchange = rabbitMQConfig.donationsTopicExchange("donaciones.exchange");
        Queue queue = rabbitMQConfig.notificationsDonationCreatedQueue("q.notificaciones.donacion-solicitud-creada");

        Binding binding = rabbitMQConfig.notificationsDonationCreatedBinding(
                queue,
                exchange,
                "donacion.solicitud.creada");

        assertThat(binding).isNotNull();
        assertThat(binding.getDestination()).isEqualTo("q.notificaciones.donacion-solicitud-creada");
        assertThat(binding.getExchange()).isEqualTo("donaciones.exchange");
        assertThat(binding.getRoutingKey()).isEqualTo("donacion.solicitud.creada");
        assertThat(binding.getDestinationType()).isEqualTo(Binding.DestinationType.QUEUE);
    }

    @Test
    @DisplayName("Debe crear cola independiente para el simulador sin interferir con la cola de producción")
    void shouldCreateSimulatorQueueAndBinding() {
        Queue queue = rabbitMQConfig.simulatorNotificationsQueue(RabbitMQConfig.SIMULATOR_QUEUE_DEFAULT);
        assertThat(queue).isNotNull();
        assertThat(queue.getName()).isEqualTo("q.donaciones.simulador-notificaciones");

        TopicExchange exchange = rabbitMQConfig.donationsTopicExchange("donaciones.exchange");
        Binding binding = rabbitMQConfig.simulatorNotificationsBinding(queue, exchange, "donacion.solicitud.creada");

        assertThat(binding).isNotNull();
        assertThat(binding.getDestination()).isEqualTo("q.donaciones.simulador-notificaciones");
        assertThat(binding.getRoutingKey()).isEqualTo("donacion.solicitud.creada");
    }

    @Test
    @DisplayName("Debe configurar Jackson2JsonMessageConverter con ObjectMapper")
    void shouldCreateJackson2JsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        Jackson2JsonMessageConverter converter = rabbitMQConfig.jackson2JsonMessageConverter(objectMapper);

        assertThat(converter).isNotNull();
    }

    @Test
    @DisplayName("Debe configurar RabbitTemplate con confirms (ACK/NACK), returns y mandatory")
    void shouldConfigureRabbitTemplateWithConfirmsAndReturns() {
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(new ObjectMapper());

        RabbitTemplate rabbitTemplate = rabbitMQConfig.rabbitTemplate(connectionFactory, converter);

        assertThat(rabbitTemplate).isNotNull();
        assertThat(rabbitTemplate.getConnectionFactory()).isSameAs(connectionFactory);
        assertThat(rabbitTemplate.getMessageConverter()).isSameAs(converter);

        // Invocar callbacks para validar robustez de registro de confirms y returns
        assertDoesNotThrow(() -> {
            CorrelationData correlationData = new CorrelationData("event-ack-1");
            rabbitMQConfig.confirmCallback().confirm(correlationData, true, null);
            rabbitMQConfig.confirmCallback().confirm(correlationData, false, "Queue full");
            rabbitMQConfig.confirmCallback().confirm(null, false, "Unknown error");

            ReturnedMessage returned = new ReturnedMessage(
                    new Message("{}".getBytes()),
                    312,
                    "NO_ROUTE",
                    "donaciones.exchange",
                    "donacion.solicitud.creada");
            rabbitMQConfig.returnsCallback().returnedMessage(returned);
        });
    }
}
