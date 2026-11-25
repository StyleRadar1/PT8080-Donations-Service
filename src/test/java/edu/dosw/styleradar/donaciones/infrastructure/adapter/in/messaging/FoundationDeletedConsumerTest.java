package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.messaging;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.core.MethodParameter;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.dosw.styleradar.donaciones.domain.port.in.HandleFoundationDeletedUseCase;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.in.messaging.event.FoundationDeletedIntegrationEvent;
import edu.dosw.styleradar.donaciones.infrastructure.config.RabbitMQConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - FoundationDeletedConsumer")
class FoundationDeletedConsumerTest {

    @Mock
    private HandleFoundationDeletedUseCase handleFoundationDeletedUseCase;

    @InjectMocks
    private FoundationDeletedConsumer consumer;

    @Test
    @DisplayName("Debe delegar al caso de uso cuando llega un evento foundation.deleted válido")
    void shouldHandleValidFoundationDeletedEvent() {
        FoundationDeletedIntegrationEvent event = FoundationDeletedIntegrationEvent.builder()
                .eventId("evt-123")
                .foundationId(3L)
                .deletedAt("2026-11-24T21:00:00Z")
                .build();
        when(handleFoundationDeletedUseCase.handleFoundationDeleted(3L)).thenReturn(2);

        consumer.onFoundationDeleted(event);

        verify(handleFoundationDeletedUseCase).handleFoundationDeleted(3L);
    }

    @Test
    @DisplayName("Debe rechazar sin reencolar (DLQ) cuando el evento o foundationId es nulo")
    void shouldRejectToDlqWhenEventOrFoundationIdIsNull() {
        assertThatThrownBy(() -> consumer.onFoundationDeleted(null))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        FoundationDeletedIntegrationEvent missingId = FoundationDeletedIntegrationEvent.builder()
                .eventId("evt-400")
                .deletedAt("2026-11-24T21:00:00Z")
                .build();
        assertThatThrownBy(() -> consumer.onFoundationDeleted(missingId))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verifyNoInteractions(handleFoundationDeletedUseCase);
    }

    @Test
    @DisplayName("Debe deserializar payload JSON de ms-tiendas ignorando cabeceras __TypeId__ externas")
    void shouldDeserializeMsTiendasPayloadIgnoringForeignTypeIdHeader() throws Exception {
        Jackson2JsonMessageConverter converter = new RabbitMQConfig().jackson2JsonMessageConverter(new ObjectMapper());
        String json = """
                {
                  "eventId": "evt-900",
                  "foundationId": 15,
                  "deletedAt": "2026-11-24T21:05:00Z",
                  "extraField": "ignored"
                }
                """;
        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        props.setHeader("__TypeId__", "edu.dosw.styleradar.tiendas.events.FoundationDeletedEvent");
        props.setInferredArgumentType(new MethodParameter(
                FoundationDeletedConsumer.class.getMethod("onFoundationDeleted", FoundationDeletedIntegrationEvent.class),
                0).getGenericParameterType());

        Message message = new Message(json.getBytes(StandardCharsets.UTF_8), props);
        Object deserialized = converter.fromMessage(message);

        assertThat(deserialized).isInstanceOf(FoundationDeletedIntegrationEvent.class);
        FoundationDeletedIntegrationEvent event = (FoundationDeletedIntegrationEvent) deserialized;
        assertThat(event.getEventId()).isEqualTo("evt-900");
        assertThat(event.getFoundationId()).isEqualTo(15L);
        assertThat(event.getDeletedAt()).isEqualTo("2026-11-24T21:05:00Z");
    }
}
