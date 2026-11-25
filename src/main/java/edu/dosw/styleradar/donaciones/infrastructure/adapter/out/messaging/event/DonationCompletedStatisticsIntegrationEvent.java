package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging.event;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.dosw.styleradar.donaciones.domain.event.DonationDeliveredEvent;
import edu.dosw.styleradar.donaciones.infrastructure.config.RabbitMQConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Evento de integración emitido en {@code styleradar.exchange} con routing key
 * {@code styleradar.donaciones.completada} cuando una fundación confirma la recepción de una donación.
 * Alimenta los indicadores de {@code ms-estadisticas} sin exponer datos personales.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DonationCompletedStatisticsIntegrationEvent implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_GARMENT_QUANTITY = 1;

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private String eventType = RabbitMQConfig.DONACION_COMPLETADA_ROUTING_KEY_DEFAULT;

    @Builder.Default
    private String occurredAt = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();

    private CompletedDonationData data;

    /**
     * Construye el payload conforme al contrato oficial de {@code ms-estadisticas}.
     */
    public static DonationCompletedStatisticsIntegrationEvent from(DonationDeliveredEvent domainEvent) {
        Instant effectiveOccurredAt = domainEvent != null && domainEvent.occurredOn() != null
                ? domainEvent.occurredOn()
                : Instant.now();
        String fundacionId = domainEvent != null && domainEvent.foundationId() != null
                ? String.valueOf(domainEvent.foundationId())
                : null;

        return DonationCompletedStatisticsIntegrationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(RabbitMQConfig.DONACION_COMPLETADA_ROUTING_KEY_DEFAULT)
                .occurredAt(effectiveOccurredAt.truncatedTo(ChronoUnit.SECONDS).toString())
                .data(CompletedDonationData.builder()
                        .fundacionId(fundacionId)
                        .cantidad(DEFAULT_GARMENT_QUANTITY)
                        .build())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CompletedDonationData implements Serializable {
        private static final long serialVersionUID = 1L;

        private String fundacionId;

        @Builder.Default
        private Integer cantidad = DEFAULT_GARMENT_QUANTITY;
    }
}
