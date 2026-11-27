package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.messaging.event;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload JSON del evento de dominio {@code foundation.deleted} publicado por {@code ms-tiendas}
 * sobre {@code styleradar.exchange} cuando un administrador elimina una fundación aliada.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FoundationDeletedIntegrationEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonAlias("eventoId")
    private String eventId;

    @JsonAlias("fundacionId")
    private Long foundationId;

    private String deletedAt;
}
