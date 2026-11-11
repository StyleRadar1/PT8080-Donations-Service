package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging.event;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.dosw.styleradar.donaciones.domain.event.DonationRequestCreatedEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload JSON del evento de integración 'donacion.solicitud.creada'.
 * Diseñado para consumo asíncrono por el microservicio ms-notificaciones (puerto 8300)
 * para el disparo automático de alertas y notificaciones push/email.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DonationRequestCreatedIntegrationEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private String eventoId = UUID.randomUUID().toString();

    @Builder.Default
    private String eventType = "styleradar.donaciones.estado.cambiado";

    @Builder.Default
    private String tipoEvento = "styleradar.donaciones.estado.cambiado";

    @Builder.Default
    private String timestamp = Instant.now().toString();

    @Builder.Default
    private String fechaHora = Instant.now().toString();

    @Builder.Default
    private String producer = "ms-segunda-vida-donaciones";

    @Builder.Default
    private String emisor = "ms-segunda-vida-donaciones";

    @Builder.Default
    private String servicio = "ms-segunda-vida-donaciones";

    @Builder.Default
    private String modulo = "DONACIONES";

    @Builder.Default
    private String correlationId = UUID.randomUUID().toString();

    private Long requestId;
    private Long publicationId;
    private Long donorId;
    private Long foundationId;

    private DonationRequestData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DonationRequestData implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long requestId;
        private Long solicitudId;
        private Long publicationId;
        private Long publicacionId;
        private Long donorId;
        private Long donanteId;
        private Long foundationId;
        private Long fundacionId;
        private String status;
        private String estado;
        private Integer version;
        private String occurredOn;
    }

    public static DonationRequestCreatedIntegrationEvent from(DonationRequestCreatedEvent domainEvent) {
        String nowStr = domainEvent.occurredOn() != null ? domainEvent.occurredOn().toString() : Instant.now().toString();
        String eventUuid = UUID.randomUUID().toString();
        String corrUuid = UUID.randomUUID().toString();

        DonationRequestData requestData = DonationRequestData.builder()
                .requestId(domainEvent.requestId())
                .solicitudId(domainEvent.requestId())
                .publicationId(domainEvent.publicationId())
                .publicacionId(domainEvent.publicationId())
                .donorId(domainEvent.donorId())
                .donanteId(domainEvent.donorId())
                .foundationId(domainEvent.foundationId())
                .fundacionId(domainEvent.foundationId())
                .status("PENDING")
                .estado("PENDIENTE")
                .version(1)
                .occurredOn(nowStr)
                .build();

        return DonationRequestCreatedIntegrationEvent.builder()
                .eventId(eventUuid)
                .eventType("donacion.solicitud.creada")
                .timestamp(nowStr)
                .producer("ms-segunda-vida-donaciones")
                .correlationId(corrUuid)
                .requestId(domainEvent.requestId())
                .publicationId(domainEvent.publicationId())
                .donorId(domainEvent.donorId())
                .foundationId(domainEvent.foundationId())
                .data(requestData)
                .build();
    }
}
