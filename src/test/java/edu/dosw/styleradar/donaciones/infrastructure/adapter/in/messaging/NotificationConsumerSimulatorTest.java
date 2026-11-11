package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.dosw.styleradar.donaciones.domain.event.DonationRequestCreatedEvent;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging.event.DonationRequestCreatedIntegrationEvent;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotificationConsumerSimulatorTest {

    @Test
    @DisplayName("Debe procesar el evento recibido sin arrojar excepciones")
    void shouldHandleReceivedEventSuccessfully() {
        NotificationConsumerSimulator simulator = new NotificationConsumerSimulator();

        DonationRequestCreatedEvent domainEvent = new DonationRequestCreatedEvent(10L, 20L, 30L, 40L);
        DonationRequestCreatedIntegrationEvent event = DonationRequestCreatedIntegrationEvent.from(domainEvent);

        assertDoesNotThrow(() -> simulator.onDonationRequestCreated(event));
    }
}
