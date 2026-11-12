package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging.event;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Evento de integración publicado por 'ms-segunda-vida-donaciones' cuando una solicitud
 * o publicación de donación cambia de estado (CREADA, ACEPTADA, RECHAZADA, RECLAMADA, PROGRAMADA, ENTREGADA).
 * 
 * Enrutado en 'styleradar.exchange' hacia:
 * 1) 'ms-notificaciones' y 'ms-estadisticas' con clave 'styleradar.donaciones.estado.cambiado'.
 * 2) Clave unificada 'styleradar.segundavida-donaciones.donacion.estado.cambiado'.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SegundaVidaDonacionEstadoCambiadoIntegrationEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String PRODUCER_NAME = "ms-segunda-vida-donaciones";

    @Builder.Default
    private String eventoId = UUID.randomUUID().toString();

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private String tipoEvento = "styleradar.donaciones.estado.cambiado";

    @Builder.Default
    private String tipoEventoUnificado = "styleradar.segundavida-donaciones.donacion.estado.cambiado";

    @Builder.Default
    private String eventType = "styleradar.donaciones.estado.cambiado";

    @Builder.Default
    private String emisor = PRODUCER_NAME;

    @Builder.Default
    private String servicio = PRODUCER_NAME;

    @Builder.Default
    private String microservicio = PRODUCER_NAME;

    @Builder.Default
    private String producer = PRODUCER_NAME;

    @Builder.Default
    private String modulo = "DONACIONES";

    @Builder.Default
    private Integer version = 1;

    @Builder.Default
    private String correlationId = UUID.randomUUID().toString();

    private Long donacionId;
    private Long solicitudId;
    private Long requestId;
    private Long publicacionId;
    private Long publicationId;
    private Long donanteId;
    private Long donorId;
    private Long foundationId;
    private Long fundacionId;
    private String estado;
    private String status;
    private String estadoAnterior;
    private String nuevoEstado;
    private String motivo;
    private String mensaje;
    private String puntoEntrega;
    private String deliveryPoint;
    private String fechaProgramada;

    private EstadoCambiadoData data;

    @Builder.Default
    private String timestamp = Instant.now().toString();

    @Builder.Default
    private String fechaEvento = Instant.now().toString();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EstadoCambiadoData implements Serializable {
        private static final long serialVersionUID = 1L;

        @Builder.Default
        private Integer version = 1;

        private Long donacionId;
        private Long solicitudId;
        private Long requestId;
        private Long donanteId;
        private Long donorId;
        private Long fundacionId;
        private Long foundationId;
        private Long publicacionId;
        private Long publicationId;
        private String estado;
        private String status;
        private String nuevoEstado;
        private String estadoAnterior;
        private String motivo;
        private String mensaje;
        private String puntoEntrega;
        private String deliveryPoint;
        private String fechaProgramada;
    }
}
