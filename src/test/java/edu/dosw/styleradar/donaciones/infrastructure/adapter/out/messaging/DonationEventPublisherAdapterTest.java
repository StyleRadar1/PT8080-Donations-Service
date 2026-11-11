package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging;

import java.time.Instant;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import edu.dosw.styleradar.donaciones.domain.event.DonationDeliveredEvent;
import edu.dosw.styleradar.donaciones.domain.event.DonationRequestCreatedEvent;
import edu.dosw.styleradar.donaciones.domain.event.DonationRequestDecidedEvent;
import edu.dosw.styleradar.donaciones.domain.event.PickupScheduledEvent;
import edu.dosw.styleradar.donaciones.domain.event.PublicationSoldEvent;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging.event.DonationRequestCreatedIntegrationEvent;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging.event.SegundaVidaDonacionEstadoCambiadoIntegrationEvent;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging.event.SegundaVidaDonacionPrendaVendidaIntegrationEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DonationEventPublisherAdapterTest {

    private RabbitTemplate rabbitTemplate;
    private DonationEventPublisherAdapter adapter;

    private static final String EXCHANGE = "styleradar.exchange";
    private static final String ROUTING_KEY_CREATED = "styleradar.donaciones.estado.cambiado";
    private static final String ROUTING_KEY_UPDATED = "styleradar.donaciones.estado.cambiado";
    private static final String ROUTING_KEY_SOLD = "styleradar.segundavida.prenda.vendida";

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        adapter = new DonationEventPublisherAdapter(
                rabbitTemplate,
                EXCHANGE,
                ROUTING_KEY_UPDATED,
                ROUTING_KEY_SOLD,
                ROUTING_KEY_CREATED);
    }

    @Test
    @DisplayName("Debe publicar evento donacion.solicitud.creada con payload de integración y CorrelationData a styleradar.exchange")
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
        assertThat(published.getEmisor()).isEqualTo("ms-segunda-vida-donaciones");
        assertThat(published.getServicio()).isEqualTo("ms-segunda-vida-donaciones");
        assertThat(published.getModulo()).isEqualTo("DONACIONES");
        assertThat(published.getRequestId()).isEqualTo(10L);
        assertThat(published.getPublicationId()).isEqualTo(20L);
        assertThat(published.getDonorId()).isEqualTo(30L);
        assertThat(published.getFoundationId()).isEqualTo(40L);

        CorrelationData correlationData = correlationCaptor.getValue();
        assertThat(correlationData).isNotNull();
        assertThat(correlationData.getId()).isEqualTo(published.getEventId());
    }

    @Test
    @DisplayName("Debe publicar evento de prenda vendida hacia ms-chat con routing key styleradar.segundavida.prenda.vendida")
    void shouldPublishPublicationSoldEvent() {
        PublicationSoldEvent event = new PublicationSoldEvent(50L, 101L);

        adapter.publishEvent(event);

        ArgumentCaptor<SegundaVidaDonacionPrendaVendidaIntegrationEvent> captor =
                ArgumentCaptor.forClass(SegundaVidaDonacionPrendaVendidaIntegrationEvent.class);
        ArgumentCaptor<CorrelationData> correlationCaptor =
                ArgumentCaptor.forClass(CorrelationData.class);

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY_SOLD), captor.capture(), correlationCaptor.capture());

        SegundaVidaDonacionPrendaVendidaIntegrationEvent published = captor.getValue();
        assertThat(published).isNotNull();
        assertThat(published.getEmisor()).isEqualTo("ms-segunda-vida-donaciones");
        assertThat(published.getServicio()).isEqualTo("ms-segunda-vida-donaciones");
        assertThat(published.getModulo()).isEqualTo("SEGUNDA_VIDA");
        assertThat(published.getPublicacionId()).isEqualTo(50L);
        assertThat(published.getVendedorId()).isEqualTo(101L);
        assertThat(published.getEstado()).isEqualTo("VENDIDA");

        CorrelationData correlationData = correlationCaptor.getValue();
        assertThat(correlationData).isNotNull();
    }

    @Test
    @DisplayName("Debe publicar evento de decisión de solicitud con routing key styleradar.donaciones.estado.cambiado")
    void shouldPublishDonationRequestDecidedEvent() {
        DonationRequestDecidedEvent event = new DonationRequestDecidedEvent(10L, 20L, true, null);

        adapter.publishEvent(event);

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY_UPDATED), any(SegundaVidaDonacionEstadoCambiadoIntegrationEvent.class), any(CorrelationData.class));
    }

    @Test
    @DisplayName("Debe publicar evento de agendamiento de recogida con routing key styleradar.donaciones.estado.cambiado")
    void shouldPublishPickupScheduledEvent() {
        PickupScheduledEvent event = new PickupScheduledEvent(
                10L,
                "Punto Central",
                LocalDateTime.of(2026, 11, 10, 10, 0),
                LocalDateTime.of(2026, 11, 10, 12, 0));

        adapter.publishEvent(event);

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY_UPDATED), any(SegundaVidaDonacionEstadoCambiadoIntegrationEvent.class), any(CorrelationData.class));
    }

    @Test
    @DisplayName("Debe publicar evento de donación entregada con routing key styleradar.donaciones.estado.cambiado")
    void shouldPublishDonationDeliveredEvent() {
        DonationDeliveredEvent event = new DonationDeliveredEvent(10L, 20L, 30L);

        adapter.publishEvent(event);

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY_UPDATED), any(SegundaVidaDonacionEstadoCambiadoIntegrationEvent.class), any(CorrelationData.class));
    }

    @Test
    @DisplayName("Debe omitir la publicación si el evento es nulo")
    void shouldIgnoreNullEvent() {
        adapter.publishEvent(null);

        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class), any(CorrelationData.class));
    }

    @Test
    @DisplayName("Debe publicar eventos genéricos con routing key por defecto y CorrelationData")
    void shouldPublishGenericEvent() {
        String genericEvent = "evento-generico";

        adapter.publishEvent(genericEvent);

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY_UPDATED), eq(genericEvent), any(CorrelationData.class));
    }
}
