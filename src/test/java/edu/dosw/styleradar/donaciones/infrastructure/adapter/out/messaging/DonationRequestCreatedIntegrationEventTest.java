package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.dosw.styleradar.donaciones.domain.event.DonationRequestCreatedEvent;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging.event.DonationRequestCreatedIntegrationEvent;

import static org.assertj.core.api.Assertions.assertThat;

class DonationRequestCreatedIntegrationEventTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Debe mapear correctamente el estado APROBADA cuando la donación proviene de un reclamo de vitrina")
    void shouldMapApprovedStatusCorrectly() {
        DonationRequestCreatedEvent domainEvent = new DonationRequestCreatedEvent(101L, 202L, 303L, 501L, "APPROVED");
        DonationRequestCreatedIntegrationEvent integrationEvent = DonationRequestCreatedIntegrationEvent.from(domainEvent);

        assertThat(integrationEvent.getData().getStatus()).isEqualTo("APPROVED");
        assertThat(integrationEvent.getData().getEstado()).isEqualTo("APROBADA");
    }

    @Test
    @DisplayName("Debe construir un evento de integración a partir del evento de dominio con todos los campos requeridos")
    void shouldBuildFromDomainEvent() {
        Instant now = Instant.parse("2026-11-08T20:00:00Z");
        DonationRequestCreatedEvent domainEvent = new DonationRequestCreatedEvent(101L, 202L, 303L, 404L, now);

        DonationRequestCreatedIntegrationEvent integrationEvent = DonationRequestCreatedIntegrationEvent.from(domainEvent);

        assertThat(integrationEvent).isNotNull();
        assertThat(integrationEvent.getEventId()).isNotBlank();
        assertThat(integrationEvent.getEventType()).isEqualTo("donacion.solicitud.creada");
        assertThat(integrationEvent.getTimestamp()).isEqualTo("2026-11-08T20:00:00Z");
        assertThat(integrationEvent.getProducer()).isEqualTo("ms-segunda-vida-donaciones");
        assertThat(integrationEvent.getCorrelationId()).isNotBlank();
        assertThat(integrationEvent.getRequestId()).isEqualTo(101L);
        assertThat(integrationEvent.getPublicationId()).isEqualTo(202L);
        assertThat(integrationEvent.getDonorId()).isEqualTo(303L);
        assertThat(integrationEvent.getFoundationId()).isEqualTo(404L);

        DonationRequestCreatedIntegrationEvent.DonationRequestData data = integrationEvent.getData();
        assertThat(data).isNotNull();
        assertThat(data.getRequestId()).isEqualTo(101L);
        assertThat(data.getSolicitudId()).isEqualTo(101L);
        assertThat(data.getPublicationId()).isEqualTo(202L);
        assertThat(data.getPublicacionId()).isEqualTo(202L);
        assertThat(data.getDonorId()).isEqualTo(303L);
        assertThat(data.getDonanteId()).isEqualTo(303L);
        assertThat(data.getFoundationId()).isEqualTo(404L);
        assertThat(data.getFundacionId()).isEqualTo(404L);
        assertThat(data.getStatus()).isEqualTo("PENDING");
        assertThat(data.getEstado()).isEqualTo("PENDIENTE");
        assertThat(data.getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("Debe serializar el payload JSON con la estructura esperada por ms-notificaciones")
    void shouldSerializeToJsonCorrectly() throws Exception {
        DonationRequestCreatedEvent domainEvent = new DonationRequestCreatedEvent(1L, 10L, 5L, 12L);
        DonationRequestCreatedIntegrationEvent event = DonationRequestCreatedIntegrationEvent.from(domainEvent);

        String json = objectMapper.writeValueAsString(event);
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.has("eventId")).isTrue();
        assertThat(root.get("eventType").asText()).isEqualTo("donacion.solicitud.creada");
        assertThat(root.get("producer").asText()).isEqualTo("ms-segunda-vida-donaciones");
        assertThat(root.get("requestId").asLong()).isEqualTo(1L);
        assertThat(root.get("publicationId").asLong()).isEqualTo(10L);
        assertThat(root.get("donorId").asLong()).isEqualTo(5L);
        assertThat(root.get("foundationId").asLong()).isEqualTo(12L);

        JsonNode data = root.get("data");
        assertThat(data).isNotNull();
        assertThat(data.get("solicitudId").asLong()).isEqualTo(1L);
        assertThat(data.get("donanteId").asLong()).isEqualTo(5L);
        assertThat(data.get("fundacionId").asLong()).isEqualTo(12L);
        assertThat(data.get("estado").asText()).isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName("Debe deserializar desde JSON al objeto de integración preservando campos")
    void shouldDeserializeFromJson() throws Exception {
        String json = """
                {
                  "eventId": "test-uuid-1234",
                  "eventType": "donacion.solicitud.creada",
                  "timestamp": "2026-11-08T22:00:00Z",
                  "producer": "ms-segunda-vida-donaciones",
                  "correlationId": "corr-uuid-5678",
                  "requestId": 99,
                  "publicationId": 88,
                  "donorId": 77,
                  "foundationId": 66,
                  "data": {
                    "solicitudId": 99,
                    "donanteId": 77,
                    "fundacionId": 66,
                    "publicacionId": 88,
                    "estado": "PENDIENTE",
                    "version": 1
                  }
                }
                """;

        DonationRequestCreatedIntegrationEvent event = objectMapper.readValue(json, DonationRequestCreatedIntegrationEvent.class);

        assertThat(event.getEventId()).isEqualTo("test-uuid-1234");
        assertThat(event.getEventType()).isEqualTo("donacion.solicitud.creada");
        assertThat(event.getRequestId()).isEqualTo(99L);
        assertThat(event.getPublicationId()).isEqualTo(88L);
        assertThat(event.getDonorId()).isEqualTo(77L);
        assertThat(event.getFoundationId()).isEqualTo(66L);
        assertThat(event.getData().getSolicitudId()).isEqualTo(99L);
        assertThat(event.getData().getEstado()).isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName("Debe soportar constructores vacíos, getters, setters y builder")
    void shouldSupportGettersSettersAndBuilder() {
        DonationRequestCreatedIntegrationEvent event = new DonationRequestCreatedIntegrationEvent();
        event.setEventId("id-1");
        event.setEventType("tipo");
        event.setTimestamp("ts");
        event.setProducer("prod");
        event.setCorrelationId("corr");
        event.setRequestId(1L);
        event.setPublicationId(2L);
        event.setDonorId(3L);
        event.setFoundationId(4L);

        DonationRequestCreatedIntegrationEvent.DonationRequestData data = new DonationRequestCreatedIntegrationEvent.DonationRequestData();
        data.setRequestId(1L);
        data.setSolicitudId(1L);
        data.setPublicationId(2L);
        data.setPublicacionId(2L);
        data.setDonorId(3L);
        data.setDonanteId(3L);
        data.setFoundationId(4L);
        data.setFundacionId(4L);
        data.setStatus("STATUS");
        data.setEstado("ESTADO");
        data.setVersion(2);
        data.setOccurredOn("occ");
        event.setData(data);

        assertThat(event.getEventId()).isEqualTo("id-1");
        assertThat(event.getEventType()).isEqualTo("tipo");
        assertThat(event.getTimestamp()).isEqualTo("ts");
        assertThat(event.getProducer()).isEqualTo("prod");
        assertThat(event.getCorrelationId()).isEqualTo("corr");
        assertThat(event.getRequestId()).isEqualTo(1L);
        assertThat(event.getPublicationId()).isEqualTo(2L);
        assertThat(event.getDonorId()).isEqualTo(3L);
        assertThat(event.getFoundationId()).isEqualTo(4L);
        assertThat(event.getData().getRequestId()).isEqualTo(1L);
        assertThat(event.getData().getSolicitudId()).isEqualTo(1L);
        assertThat(event.getData().getPublicationId()).isEqualTo(2L);
        assertThat(event.getData().getPublicacionId()).isEqualTo(2L);
        assertThat(event.getData().getDonorId()).isEqualTo(3L);
        assertThat(event.getData().getDonanteId()).isEqualTo(3L);
        assertThat(event.getData().getFoundationId()).isEqualTo(4L);
        assertThat(event.getData().getFundacionId()).isEqualTo(4L);
        assertThat(event.getData().getStatus()).isEqualTo("STATUS");
        assertThat(event.getData().getEstado()).isEqualTo("ESTADO");
        assertThat(event.getData().getVersion()).isEqualTo(2);
        assertThat(event.getData().getOccurredOn()).isEqualTo("occ");
        assertThat(event.toString()).isNotBlank();
        assertThat(data.toString()).isNotBlank();
        assertThat(event.hashCode()).isNotZero();
        assertThat(data.hashCode()).isNotZero();
    }
}
