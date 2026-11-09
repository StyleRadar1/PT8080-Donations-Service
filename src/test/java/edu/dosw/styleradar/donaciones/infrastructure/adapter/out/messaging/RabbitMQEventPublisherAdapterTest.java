package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import edu.dosw.styleradar.donaciones.domain.event.DonationRequestCreatedEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitMQEventPublisherAdapterTest {

    @Test
    @SuppressWarnings("deprecation")
    @DisplayName("Debe mantener compatibilidad con la clase heredada RabbitMQEventPublisherAdapter")
    void shouldMaintainCompatibilityWithLegacyAdapter() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RabbitMQEventPublisherAdapter adapter = new RabbitMQEventPublisherAdapter(
                rabbitTemplate,
                "donaciones.exchange",
                "donacion.solicitud.creada",
                "donacion.estado.actualizado");

        DonationRequestCreatedEvent event = new DonationRequestCreatedEvent(5L, 6L, 7L, 8L);
        adapter.publishEvent(event);

        verify(rabbitTemplate).convertAndSend(eq("donaciones.exchange"), eq("donacion.solicitud.creada"), any(Object.class));
        assertThat(adapter.getExchangeName()).isEqualTo("donaciones.exchange");
    }
}
