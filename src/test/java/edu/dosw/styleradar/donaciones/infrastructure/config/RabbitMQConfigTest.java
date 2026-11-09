package edu.dosw.styleradar.donaciones.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

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
    @DisplayName("Debe crear cola durable q.notificaciones.donacion-solicitud-creada")
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
    @DisplayName("Debe configurar Jackson2JsonMessageConverter con ObjectMapper")
    void shouldCreateJackson2JsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        Jackson2JsonMessageConverter converter = rabbitMQConfig.jackson2JsonMessageConverter(objectMapper);

        assertThat(converter).isNotNull();
    }

    @Test
    @DisplayName("Debe configurar RabbitTemplate con ConnectionFactory y convertidor JSON")
    void shouldConfigureRabbitTemplate() {
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(new ObjectMapper());

        RabbitTemplate rabbitTemplate = rabbitMQConfig.rabbitTemplate(connectionFactory, converter);

        assertThat(rabbitTemplate).isNotNull();
        assertThat(rabbitTemplate.getConnectionFactory()).isSameAs(connectionFactory);
        assertThat(rabbitTemplate.getMessageConverter()).isSameAs(converter);
    }
}
