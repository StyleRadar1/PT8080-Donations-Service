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

    @Builder.Default
    private String eventoId = UUID.randomUUID().toString();

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private String tipoEvento = "segunda-mano.prenda.vendida";

    @Builder.Default
    private String tipoEventoUnificado = "styleradar.segundavida-donaciones.prenda.vendida";

    @Builder.Default
    private String eventType = "segunda-mano.prenda.vendida";

    @Builder.Default
    private String emisor = "ms-segunda-vida-donaciones";

    @Builder.Default
    private String servicio = "ms-segunda-vida-donaciones";

    @Builder.Default
    private String microservicio = "ms-segunda-vida-donaciones";

    @Builder.Default
    private String producer = "ms-segunda-vida-donaciones";

    @Builder.Default
    private String modulo = "SEGUNDA_VIDA";

    @Builder.Default
    private String canal = "C2C";

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
        PrendaVendidaData dataEnvelope = PrendaVendidaData.builder()
                .publicacionId(domainEvent.publicationId())
                .prendaId(domainEvent.publicationId())
                .vendedorId(domainEvent.userId())
                .compradorId(domainEvent.compradorId())
                .estado("VENDIDA")
                .moneda("COP")
                .fechaVenta(now)
                .build();

        return SegundaVidaDonacionPrendaVendidaIntegrationEvent.builder()
                .eventoId(UUID.randomUUID().toString())
                .eventId(UUID.randomUUID().toString())
                .tipoEvento("segunda-mano.prenda.vendida")
                .tipoEventoUnificado("styleradar.segundavida-donaciones.prenda.vendida")
                .eventType("segunda-mano.prenda.vendida")
                .emisor("ms-segunda-vida-donaciones")
                .servicio("ms-segunda-vida-donaciones")
                .microservicio("ms-segunda-vida-donaciones")
                .producer("ms-segunda-vida-donaciones")
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
