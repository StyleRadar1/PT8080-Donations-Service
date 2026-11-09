package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class RabbitMQEventPublisherAdapterTest {

    private final RabbitMQEventPublisherAdapter adapter = new RabbitMQEventPublisherAdapter();

    @Test
    @DisplayName("Debe publicar eventos de dominio sin lanzar excepciones")
    void shouldPublishEventWithoutError() {
        assertDoesNotThrow(() -> adapter.publishEvent("evento-prueba"));
    }
}
