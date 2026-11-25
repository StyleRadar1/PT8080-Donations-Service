package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.core.ParameterizedTypeReference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.dosw.styleradar.donaciones.domain.event.DonationDeliveredEvent;
import edu.dosw.styleradar.donaciones.domain.event.DonationRequestCreatedEvent;
import edu.dosw.styleradar.donaciones.domain.event.DonationRequestDecidedEvent;
import edu.dosw.styleradar.donaciones.domain.event.PickupScheduledEvent;
import edu.dosw.styleradar.donaciones.domain.event.PublicationSoldEvent;
import edu.dosw.styleradar.donaciones.domain.exception.ExternalServiceUnavailableException;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging.event.DonationCompletedStatisticsIntegrationEvent;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging.event.DonationRequestCreatedIntegrationEvent;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging.event.SegundaVidaDonacionEstadoCambiadoIntegrationEvent;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging.event.SegundaVidaDonacionPrendaVendidaIntegrationEvent;
import edu.dosw.styleradar.donaciones.infrastructure.config.RabbitMQConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DonationEventPublisherAdapterTest {

    private RabbitTemplate rabbitTemplate;
    private DonationEventPublisherAdapter adapter;

    private static final String EXCHANGE = "styleradar.exchange";
    private static final String CHAT_EXCHANGE = "styleradar.exchange";
    private static final String ROUTING_KEY_CREATED = "styleradar.donaciones.solicitud.creada";
    private static final String ROUTING_KEY_UPDATED = "styleradar.donaciones.estado.cambiado";
    private static final String ROUTING_KEY_COMPLETED = "styleradar.donaciones.completada";
    private static final String ROUTING_KEY_SOLD = "segunda-mano.prenda.vendida";
    private static final String ROUTING_KEY_SOLD_NOTIFICATIONS = "styleradar.segundavida.prenda.vendida";

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        adapter = new DonationEventPublisherAdapter(
                rabbitTemplate,
                EXCHANGE,
                CHAT_EXCHANGE,
                ROUTING_KEY_UPDATED,
                ROUTING_KEY_SOLD,
                ROUTING_KEY_SOLD_NOTIFICATIONS,
                ROUTING_KEY_CREATED);
    }

    @Test
    @DisplayName("Debe publicar evento donacion.solicitud.creada con donacionId y donanteId para ms-notificaciones")
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
        assertThat(published.getDonacionId()).isEqualTo(10L);
        assertThat(published.getDonanteId()).isEqualTo(30L);
        assertThat(published.getPublicationId()).isEqualTo(20L);
        assertThat(published.getDonorId()).isEqualTo(30L);
        assertThat(published.getFoundationId()).isEqualTo(40L);
        assertThat(published.getData()).isNotNull();
        assertThat(published.getData().getDonacionId()).isEqualTo(10L);
        assertThat(published.getData().getDonanteId()).isEqualTo(30L);

        CorrelationData correlationData = correlationCaptor.getValue();
        assertThat(correlationData).isNotNull();
        assertThat(correlationData.getId()).isEqualTo(published.getEventId());
    }

    @Test
    @DisplayName("Debe publicar evento de prenda vendida hacia ms-chat y ms-notificaciones en styleradar.exchange con sus routing keys e interesadosIds")
    void shouldPublishPublicationSoldEvent() {
        PublicationSoldEvent event = new PublicationSoldEvent(50L, 101L, 102L, List.of(201L, 202L, 101L, 102L, 201L));

        adapter.publishEvent(event);

        ArgumentCaptor<SegundaVidaDonacionPrendaVendidaIntegrationEvent> chatCaptor =
                ArgumentCaptor.forClass(SegundaVidaDonacionPrendaVendidaIntegrationEvent.class);
        ArgumentCaptor<SegundaVidaDonacionPrendaVendidaIntegrationEvent> notifCaptor =
                ArgumentCaptor.forClass(SegundaVidaDonacionPrendaVendidaIntegrationEvent.class);
        ArgumentCaptor<CorrelationData> correlationCaptor =
                ArgumentCaptor.forClass(CorrelationData.class);

        verify(rabbitTemplate).convertAndSend(eq(CHAT_EXCHANGE), eq(ROUTING_KEY_SOLD), chatCaptor.capture(), correlationCaptor.capture());
        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY_SOLD_NOTIFICATIONS), notifCaptor.capture(), any(CorrelationData.class));

        SegundaVidaDonacionPrendaVendidaIntegrationEvent published = chatCaptor.getValue();
        assertThat(published).isNotNull();
        assertThat(published.getEventType()).isEqualTo("segunda-mano.prenda.vendida");
        assertThat(published.getEmisor()).isEqualTo("ms-segunda-vida-donaciones");
        assertThat(published.getServicio()).isEqualTo("ms-segunda-vida-donaciones");
        assertThat(published.getModulo()).isEqualTo("SEGUNDA_VIDA");
        assertThat(published.getPublicacionId()).isEqualTo(50L);
        assertThat(published.getVendedorId()).isEqualTo(101L);
        assertThat(published.getCompradorId()).isEqualTo(102L);
        assertThat(published.getInteresadosIds()).containsExactly(201L, 202L);
        assertThat(published.getEstado()).isEqualTo("VENDIDA");

        // Envelope data para ms-chat y ms-notificaciones
        assertThat(published.getData()).isNotNull();
        assertThat(published.getData().getPublicacionId()).isEqualTo(50L);
        assertThat(published.getData().getVendedorId()).isEqualTo(101L);
        assertThat(published.getData().getCompradorId()).isEqualTo(102L);
        assertThat(published.getData().getInteresadosIds()).containsExactly(201L, 202L);
        assertThat(published.getData().getEstado()).isEqualTo("VENDIDA");

        assertThat(notifCaptor.getValue().getInteresadosIds()).containsExactly(201L, 202L);
        assertThat(correlationCaptor.getValue()).isNotNull();
        assertThat(adapter.getChatExchangeName()).isEqualTo("styleradar.exchange");
        assertThat(adapter.getNotificationsSecondHandSoldRoutingKey()).isEqualTo("styleradar.segundavida.prenda.vendida");
    }

    @Test
    @DisplayName("Debe serializar y deserializar contratos reales JSON de solicitud creada, estado cambiado y prenda vendida")
    void shouldPreserveJsonContractsOnSerializationAndConsumption() {
        Jackson2JsonMessageConverter converter = new RabbitMQConfig().jackson2JsonMessageConverter(new ObjectMapper());

        DonationRequestCreatedIntegrationEvent createdPayload = DonationRequestCreatedIntegrationEvent.from(
                new DonationRequestCreatedEvent(10L, 20L, 30L, 40L, "PENDING", 24L));
        Message createdMsg = converter.toMessage(createdPayload, new MessageProperties());
        DonationRequestCreatedIntegrationEvent consumedCreated = (DonationRequestCreatedIntegrationEvent) converter.fromMessage(
                createdMsg, new ParameterizedTypeReference<DonationRequestCreatedIntegrationEvent>() {}.getType());

        assertThat(consumedCreated.getRepresentanteId()).isEqualTo(24L);
        assertThat(consumedCreated.getFundacionUsuarioId()).isEqualTo(24L);
        assertThat(consumedCreated.getData().getRepresentanteId()).isEqualTo(24L);
        assertThat(consumedCreated.getData().getFundacionUsuarioId()).isEqualTo(24L);

        SegundaVidaDonacionEstadoCambiadoIntegrationEvent statusPayload =
                SegundaVidaDonacionEstadoCambiadoIntegrationEvent.builder()
                        .donacionId(10L)
                        .solicitudId(10L)
                        .donanteId(30L)
                        .fundacionId(40L)
                        .nuevoEstado("APROBADA")
                        .build();
        Message statusMsg = converter.toMessage(statusPayload, new MessageProperties());
        SegundaVidaDonacionEstadoCambiadoIntegrationEvent consumedStatus =
                (SegundaVidaDonacionEstadoCambiadoIntegrationEvent) converter.fromMessage(
                        statusMsg, new ParameterizedTypeReference<SegundaVidaDonacionEstadoCambiadoIntegrationEvent>() {}.getType());
        assertThat(consumedStatus.getDonacionId()).isEqualTo(10L);
        assertThat(consumedStatus.getNuevoEstado()).isEqualTo("APROBADA");

        SegundaVidaDonacionPrendaVendidaIntegrationEvent soldPayload = SegundaVidaDonacionPrendaVendidaIntegrationEvent.from(
                new PublicationSoldEvent(50L, 101L, 102L, List.of(201L, 202L), "Chaqueta bomber"));
        Message soldMsg = converter.toMessage(soldPayload, new MessageProperties());
        SegundaVidaDonacionPrendaVendidaIntegrationEvent consumedSold =
                (SegundaVidaDonacionPrendaVendidaIntegrationEvent) converter.fromMessage(
                        soldMsg, new ParameterizedTypeReference<SegundaVidaDonacionPrendaVendidaIntegrationEvent>() {}.getType());

        assertThat(consumedSold.getPublicacionId()).isEqualTo(50L);
        assertThat(consumedSold.getNombrePrenda()).isEqualTo("Chaqueta bomber");
        assertThat(consumedSold.getVendedorId()).isEqualTo(101L);
        assertThat(consumedSold.getCompradorId()).isEqualTo(102L);
        assertThat(consumedSold.getInteresadosIds()).containsExactly(201L, 202L);
        assertThat(consumedSold.getFechaEvento()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
        assertThat(LocalDateTime.parse(consumedSold.getFechaEvento())).isNotNull();
        assertThat(consumedSold.getData().getInteresadosIds()).containsExactly(201L, 202L);
    }

    @Test
    @DisplayName("Debe publicar evento de decisión de solicitud con donacionId y donanteId para ms-notificaciones")
    void shouldPublishDonationRequestDecidedEvent() {
        DonationRequestDecidedEvent event = new DonationRequestDecidedEvent(10L, 20L, 30L, true, null);

        adapter.publishEvent(event);

        ArgumentCaptor<SegundaVidaDonacionEstadoCambiadoIntegrationEvent> captor =
                ArgumentCaptor.forClass(SegundaVidaDonacionEstadoCambiadoIntegrationEvent.class);

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY_UPDATED), captor.capture(), any(CorrelationData.class));

        SegundaVidaDonacionEstadoCambiadoIntegrationEvent published = captor.getValue();
        assertThat(published.getDonacionId()).isEqualTo(10L);
        assertThat(published.getDonanteId()).isEqualTo(30L);
        assertThat(published.getNuevoEstado()).isEqualTo("APROBADA");
        assertThat(published.getFechaCambio()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
        assertThat(published.getFechaEvento()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
        assertThat(LocalDateTime.parse(published.getFechaCambio())).isNotNull();
        assertThat(published.getData()).isNotNull();
        assertThat(published.getData().getDonacionId()).isEqualTo(10L);
        assertThat(published.getData().getDonanteId()).isEqualTo(30L);
        assertThat(published.getData().getFechaCambio()).isEqualTo(published.getFechaCambio());
    }

    @Test
    @DisplayName("Debe publicar evento de agendamiento de recogida con estado CITA_PROGRAMADA, donacionId y fechaCambio ISO-8601 sin zona")
    void shouldPublishPickupScheduledEvent() {
        PickupScheduledEvent event = new PickupScheduledEvent(
                10L,
                99L,
                "Punto Central",
                LocalDateTime.of(2026, 11, 10, 10, 0),
                LocalDateTime.of(2026, 11, 10, 12, 0));

        adapter.publishEvent(event);

        ArgumentCaptor<SegundaVidaDonacionEstadoCambiadoIntegrationEvent> captor =
                ArgumentCaptor.forClass(SegundaVidaDonacionEstadoCambiadoIntegrationEvent.class);
        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY_UPDATED), captor.capture(), any(CorrelationData.class));

        SegundaVidaDonacionEstadoCambiadoIntegrationEvent published = captor.getValue();
        assertThat(published.getDonacionId()).isEqualTo(10L);
        assertThat(published.getDonanteId()).isEqualTo(99L);
        assertThat(published.getNuevoEstado()).isEqualTo("CITA_PROGRAMADA");
        assertThat(published.getNuevoEstado()).contains("CITA");
        assertThat(published.getMotivo()).isEqualTo("Punto de entrega: Punto Central");
        assertThat(published.getFechaCambio()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
        assertThat(LocalDateTime.parse(published.getFechaCambio())).isNotNull();
        assertThat(published.getData().getDonacionId()).isEqualTo(10L);
        assertThat(published.getData().getDonanteId()).isEqualTo(99L);
        assertThat(published.getData().getNuevoEstado()).isEqualTo("CITA_PROGRAMADA");
    }

    @Test
    @DisplayName("Debe publicar evento de donación entregada para ms-notificaciones y ms-estadisticas")
    void shouldPublishDonationDeliveredEvent() throws Exception {
        Instant occurredOn = Instant.parse("2026-11-24T15:00:00.123Z");
        DonationDeliveredEvent event = new DonationDeliveredEvent(10L, 20L, 30L, 99L, occurredOn);

        adapter.publishEvent(event);

        ArgumentCaptor<SegundaVidaDonacionEstadoCambiadoIntegrationEvent> notificationsCaptor =
                ArgumentCaptor.forClass(SegundaVidaDonacionEstadoCambiadoIntegrationEvent.class);
        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY_UPDATED), notificationsCaptor.capture(), any(CorrelationData.class));

        SegundaVidaDonacionEstadoCambiadoIntegrationEvent published = notificationsCaptor.getValue();
        assertThat(published.getDonacionId()).isEqualTo(10L);
        assertThat(published.getDonanteId()).isEqualTo(99L);
        assertThat(published.getData().getDonacionId()).isEqualTo(10L);
        assertThat(published.getData().getDonanteId()).isEqualTo(99L);

        ArgumentCaptor<DonationCompletedStatisticsIntegrationEvent> statsCaptor =
                ArgumentCaptor.forClass(DonationCompletedStatisticsIntegrationEvent.class);
        ArgumentCaptor<CorrelationData> statsCorrelationCaptor =
                ArgumentCaptor.forClass(CorrelationData.class);
        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY_COMPLETED), statsCaptor.capture(), statsCorrelationCaptor.capture());

        DonationCompletedStatisticsIntegrationEvent statsEvent = statsCaptor.getValue();
        assertThat(statsEvent.getEventId()).isNotBlank();
        assertThat(statsCorrelationCaptor.getValue().getId()).isEqualTo(statsEvent.getEventId());
        assertThat(statsEvent.getEventType()).isEqualTo("styleradar.donaciones.completada");
        assertThat(statsEvent.getOccurredAt()).isEqualTo("2026-11-24T15:00:00Z");
        assertThat(statsEvent.getData()).isNotNull();
        assertThat(statsEvent.getData().getFundacionId()).isEqualTo("30");
        assertThat(statsEvent.getData().getCantidad()).isEqualTo(1);
        assertThat(adapter.getDonationCompletedRoutingKey()).isEqualTo(ROUTING_KEY_COMPLETED);

        JsonNode json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(statsEvent));
        assertThat(json.path("eventId").asText()).isEqualTo(statsEvent.getEventId());
        assertThat(json.path("eventType").asText()).isEqualTo("styleradar.donaciones.completada");
        assertThat(json.path("occurredAt").asText()).isEqualTo("2026-11-24T15:00:00Z");
        assertThat(json.path("data").path("fundacionId").asText()).isEqualTo("30");
        assertThat(json.path("data").path("cantidad").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("Debe omitir publicación a ms-estadisticas si foundationId es nulo pero mantener notificación")
    void shouldSkipStatisticsEventWhenFoundationIdIsNull() {
        DonationDeliveredEvent event = new DonationDeliveredEvent(10L, 20L, null, 99L);

        adapter.publishEvent(event);

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY_UPDATED),
                any(SegundaVidaDonacionEstadoCambiadoIntegrationEvent.class), any(CorrelationData.class));
        verify(rabbitTemplate, never()).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY_COMPLETED),
                any(Object.class), any(CorrelationData.class));
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

    @Test
    @DisplayName("Debe preservar el eventId explícito en payload y CorrelationData para entregas idempotentes")
    void shouldPreserveExplicitEventIdInDonationRequestDecidedEvent() {
        DonationRequestDecidedEvent domainEvent = new DonationRequestDecidedEvent(
                10L, 20L, 30L, false, "Fundación eliminada", Instant.parse("2026-11-24T22:00:00Z"), "evt-fixed-123");

        adapter.publishEvent(domainEvent);

        ArgumentCaptor<SegundaVidaDonacionEstadoCambiadoIntegrationEvent> captor =
                ArgumentCaptor.forClass(SegundaVidaDonacionEstadoCambiadoIntegrationEvent.class);
        ArgumentCaptor<CorrelationData> correlationCaptor =
                ArgumentCaptor.forClass(CorrelationData.class);

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY_UPDATED), captor.capture(), correlationCaptor.capture());

        SegundaVidaDonacionEstadoCambiadoIntegrationEvent published = captor.getValue();
        assertThat(published.getEventoId()).isEqualTo("evt-fixed-123");
        assertThat(published.getEventId()).isEqualTo("evt-fixed-123");
        assertThat(published.getCorrelationId()).isEqualTo("evt-fixed-123");
        assertThat(correlationCaptor.getValue().getId()).isEqualTo("evt-fixed-123");
    }

    @Test
    @DisplayName("publishEventConfirmed debe completarse únicamente tras recibir ACK del broker sin devolución")
    void shouldSucceedPublishEventConfirmedWhenBrokerAcks() {
        DonationRequestDecidedEvent domainEvent = new DonationRequestDecidedEvent(
                10L, 20L, 30L, false, "Fundación eliminada", Instant.now(), "evt-ack-1");

        doAnswer(invocation -> {
            CorrelationData cd = invocation.getArgument(3);
            cd.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class), any(CorrelationData.class));

        adapter.publishEventConfirmed(domainEvent);

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY_UPDATED), any(Object.class), any(CorrelationData.class));
    }

    @Test
    @DisplayName("publishEventConfirmed debe lanzar ExternalServiceUnavailableException cuando el broker responde NACK")
    void shouldThrowWhenBrokerNacksOnPublishEventConfirmed() {
        DonationRequestDecidedEvent domainEvent = new DonationRequestDecidedEvent(
                10L, 20L, 30L, false, "Fundación eliminada", Instant.now(), "evt-nack-1");

        doAnswer(invocation -> {
            CorrelationData cd = invocation.getArgument(3);
            cd.getFuture().complete(new CorrelationData.Confirm(false, "channel closed"));
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class), any(CorrelationData.class));

        assertThatThrownBy(() -> adapter.publishEventConfirmed(domainEvent))
                .isInstanceOf(ExternalServiceUnavailableException.class)
                .hasMessageContaining("NACK");
    }

    @Test
    @DisplayName("publishEventConfirmed debe fallar si RabbitMQ devuelve el mensaje como no enrutable")
    void shouldThrowWhenBrokerReturnsUnroutableMessageOnPublishEventConfirmed() {
        DonationRequestDecidedEvent domainEvent = new DonationRequestDecidedEvent(
                10L, 20L, 30L, false, "Fundación eliminada", Instant.now(), "evt-return-1");

        doAnswer(invocation -> {
            CorrelationData cd = invocation.getArgument(3);
            cd.setReturned(new ReturnedMessage(
                    new Message(new byte[0], new MessageProperties()),
                    312,
                    "NO_ROUTE",
                    EXCHANGE,
                    ROUTING_KEY_UPDATED));
            cd.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class), any(CorrelationData.class));

        assertThatThrownBy(() -> adapter.publishEventConfirmed(domainEvent))
                .isInstanceOf(ExternalServiceUnavailableException.class)
                .hasMessageContaining("devuelto por RabbitMQ");
    }

    @Test
    @DisplayName("publishEventConfirmed debe fallar por timeout cuando no llega confirmación del broker y omitir nulos")
    void shouldThrowWhenBrokerConfirmTimesOutAndIgnoreNull() {
        adapter.publishEventConfirmed(null);
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class), any(CorrelationData.class));

        adapter.setConfirmTimeoutMs(25L);
        DonationRequestDecidedEvent domainEvent = new DonationRequestDecidedEvent(
                10L, 20L, 30L, false, "Fundación eliminada", Instant.now(), "evt-timeout-1");

        assertThatThrownBy(() -> adapter.publishEventConfirmed(domainEvent))
                .isInstanceOf(ExternalServiceUnavailableException.class)
                .hasMessageContaining("confirmación");
    }
}
