package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging.event;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.dosw.styleradar.donaciones.domain.event.PublicationSoldEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Evento de integración publicado por 'ms-segunda-vida-donaciones' cuando una prenda
 * de segunda mano es marcada como VENDIDA.
 * 
 * Enrutado en 'styleradar.exchange' hacia:
 * 1) 'ms-chat' (cola 'q.chat.segunda-mano') con clave 'styleradar.segundavida.prenda.vendida' para cerrar salas de chat activas.
 * 2) Clave unificada 'styleradar.segundavida-donaciones.prenda.vendida'.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SegundaVidaDonacionPrendaVendidaIntegrationEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String SERVICE_NAME = "ms-segunda-vida-donaciones";
    public static final String EVENT_TYPE_SOLD = "segunda-mano.prenda.vendida";

    @Builder.Default
    private String eventoId = UUID.randomUUID().toString();

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private String tipoEvento = EVENT_TYPE_SOLD;

    @Builder.Default
    private String tipoEventoUnificado = "styleradar.segundavida-donaciones.prenda.vendida";

    @Builder.Default
    private String eventType = EVENT_TYPE_SOLD;

    @Builder.Default
    private String emisor = SERVICE_NAME;

    @Builder.Default
    private String servicio = SERVICE_NAME;

    @Builder.Default
    private String microservicio = SERVICE_NAME;

    @Builder.Default
    private String producer = SERVICE_NAME;

    @Builder.Default
    private String modulo = "SEGUNDA_VIDA";

    @Builder.Default
    private String canal = "C2C";

    @Builder.Default
    private Integer version = 1;

    @Builder.Default
    private String correlationId = UUID.randomUUID().toString();

    private Long publicacionId;
    private Long prendaId;
    private Long vendedorId;
    private Long compradorId;
    private Double precioFinal;
    private String moneda;
    private String estado;
    private String fechaVenta;

    private PrendaVendidaData data;

    @Builder.Default
    private String timestamp = Instant.now().toString();

    @Builder.Default
    private String fechaEvento = Instant.now().toString();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PrendaVendidaData implements Serializable {
        private static final long serialVersionUID = 1L;

        @Builder.Default
        private Integer version = 1;

        private Long publicacionId;
        private Long prendaId;
        private Long vendedorId;
        private Long compradorId;
        private Double precioFinal;
        private String moneda;
        private String estado;
        private String fechaVenta;
    }

    public static SegundaVidaDonacionPrendaVendidaIntegrationEvent from(PublicationSoldEvent domainEvent) {
        String now = domainEvent.occurredOn() != null ? domainEvent.occurredOn().toString() : Instant.now().toString();
        String corrId = UUID.randomUUID().toString();
        String evtId = UUID.randomUUID().toString();

        PrendaVendidaData dataEnvelope = PrendaVendidaData.builder()
                .version(1)
                .publicacionId(domainEvent.publicationId())
                .prendaId(domainEvent.publicationId())
                .vendedorId(domainEvent.userId())
                .compradorId(domainEvent.compradorId())
                .estado("VENDIDA")
                .moneda("COP")
                .fechaVenta(now)
                .build();

        return SegundaVidaDonacionPrendaVendidaIntegrationEvent.builder()
                .eventoId(evtId)
                .eventId(evtId)
                .correlationId(corrId)
                .version(1)
                .tipoEvento(EVENT_TYPE_SOLD)
                .tipoEventoUnificado("styleradar.segundavida-donaciones.prenda.vendida")
                .eventType(EVENT_TYPE_SOLD)
                .emisor(SERVICE_NAME)
                .servicio(SERVICE_NAME)
                .microservicio(SERVICE_NAME)
                .producer(SERVICE_NAME)
                .modulo("SEGUNDA_VIDA")
                .canal("C2C")
                .publicacionId(domainEvent.publicationId())
                .prendaId(domainEvent.publicationId())
                .vendedorId(domainEvent.userId())
                .compradorId(domainEvent.compradorId())
                .estado("VENDIDA")
                .moneda("COP")
                .fechaVenta(now)
                .timestamp(now)
                .fechaEvento(now)
                .data(dataEnvelope)
                .build();
    }
}
