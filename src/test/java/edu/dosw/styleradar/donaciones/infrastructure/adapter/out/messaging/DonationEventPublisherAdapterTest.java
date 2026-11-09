package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging;

import java.time.Instant;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import edu.dosw.styleradar.donaciones.domain.event.DonationDeliveredEvent;
import edu.dosw.styleradar.donaciones.domain.event.DonationRequestCreatedEvent;
import edu.dosw.styleradar.donaciones.domain.event.DonationRequestDecidedEvent;
import edu.dosw.styleradar.donaciones.domain.event.PickupScheduledEvent;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging.event.DonationRequestCreatedIntegrationEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DonationEventPublisherAdapterTest {

    private RabbitTemplate rabbitTemplate;
    private DonationEventPublisherAdapter adapter;

    private static final String EXCHANGE = "donaciones.exchange";
    private static final String ROUTING_KEY_CREATED = "donacion.solicitud.creada";
    private static final String ROUTING_KEY_UPDATED = "donacion.estado.actualizado";

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        adapter = new DonationEventPublisherAdapter(
                rabbitTemplate,
                EXCHANGE,
                ROUTING_KEY_CREATED,
                ROUTING_KEY_UPDATED);
    }

    @Test
    @DisplayName("Debe publicar evento donacion.solicitud.creada con payload de integración y CorrelationData a donaciones.exchange")
    void shouldPublishDonationRequestCreatedEvent() {
        Instant now = Instant.parse("2026-11-08T18:00:00Z");
        DonationRequestCreatedEvent domainEvent = new DonationRequestCreatedEvent(10L, 20L, 30L, 40L, now);

        adapter.publishEvent(domainEvent);

        ArgumentCaptor<DonationRequestCreatedIntegrationEvent> captor =
                ArgumentCaptor.forClass(DonationRequestCreatedIntegrationEvent.class);
        ArgumentCaptor<CorrelationData> correlationCaptor =
                ArgumentCaptor.forClass(CorrelationData.class);

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY_CREATED), captor.capture(), correlationCaptor.capture());

        DonationRequestCreatedIntegrationEvent published = captor.getValue();
        assertThat(published).isNotNull();
        assertThat(published.getEventType()).isEqualTo("donacion.solicitud.creada");
        assertThat(published.getRequestId()).isEqualTo(10L);
        assertThat(published.getPublicationId()).isEqualTo(20L);
        assertThat(published.getDonorId()).isEqualTo(30L);
        assertThat(published.getFoundationId()).isEqualTo(40L);
        assertThat(published.getData().getSolicitudId()).isEqualTo(10L);
        assertThat(published.getData().getDonanteId()).isEqualTo(30L);

        CorrelationData correlationData = correlationCaptor.getValue();
        assertThat(correlationData).isNotNull();
        assertThat(correlationData.getId()).isEqualTo(published.getEventId());
    }

    @Test
    @DisplayName("Debe publicar evento de decisión de solicitud con routing key donacion.estado.actualizado y CorrelationData")
    void shouldPublishDonationRequestDecidedEvent() {
        DonationRequestDecidedEvent event = new DonationRequestDecidedEvent(10L, 20L, true, null);

        adapter.publishEvent(event);

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY_UPDATED), eq(event), any(CorrelationData.class));
    }

    @Test
    @DisplayName("Debe publicar evento de agendamiento de recogida con routing key donacion.estado.actualizado y CorrelationData")
    void shouldPublishPickupScheduledEvent() {
        PickupScheduledEvent event = new PickupScheduledEvent(
                10L,
                "Punto Central",
                LocalDateTime.of(2026, 11, 10, 10, 0),
                LocalDateTime.of(2026, 11, 10, 12, 0));

        adapter.publishEvent(event);

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY_UPDATED), eq(event), any(CorrelationData.class));
    }

    @Test
    @DisplayName("Debe publicar evento de donación entregada con routing key donacion.estado.actualizado y CorrelationData")
    void shouldPublishDonationDeliveredEvent() {
        DonationDeliveredEvent event = new DonationDeliveredEvent(10L, 20L, 30L);

        adapter.publishEvent(event);

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY_UPDATED), eq(event), any(CorrelationData.class));
    }

    @Test
    @DisplayName("Debe omitir la publicación si el evento es nulo")
    void shouldIgnoreNullEvent() {
        adapter.publishEvent(null);

        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class), any(CorrelationData.class));
    }

    @Test
    @DisplayName("Debe publicar eventos genéricos con routing key donaciones.general y CorrelationData")
    void shouldPublishGenericEvent() {
        String genericEvent = "evento-generico";

        adapter.publishEvent(genericEvent);

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq("donaciones.general"), eq(genericEvent), any(CorrelationData.class));
    }

    @Test
    @DisplayName("Debe propagar AmqpException cuando falla la comunicación con el broker RabbitMQ")
    void shouldThrowAmqpExceptionOnError() {
        DonationRequestCreatedEvent domainEvent = new DonationRequestCreatedEvent(1L, 2L, 3L, 4L);

        doThrow(new AmqpConnectException(new java.net.ConnectException("Connection refused")))
                .when(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY_CREATED), any(Object.class), any(CorrelationData.class));

        assertThatThrownBy(() -> adapter.publishEvent(domainEvent))
                .isInstanceOf(AmqpConnectException.class);
    }

    @Test
    @DisplayName("Debe exponer getters para configuración de exchange y routing keys")
    void shouldExposeConfigurationGetters() {
        assertThat(adapter.getExchangeName()).isEqualTo(EXCHANGE);
        assertThat(adapter.getDonationRequestCreatedRoutingKey()).isEqualTo(ROUTING_KEY_CREATED);
        assertThat(adapter.getDonationStatusUpdatedRoutingKey()).isEqualTo(ROUTING_KEY_UPDATED);
    }
}
