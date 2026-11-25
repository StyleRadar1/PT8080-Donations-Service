package edu.dosw.styleradar.donaciones.domain.port.in;

import java.time.LocalDateTime;

import edu.dosw.styleradar.donaciones.domain.model.DeliveryMode;

/**
 * Comando para radicar una solicitud de donación sobre una prenda publicada hacia una fundación,
 * incluyendo la propuesta inicial de entrega (SR-RF-45 / SR-RF-48).
 */
public record CreateDonationRequestCommand(
        Long donorId,
        Long publicationId,
        Long foundationId,
        String message,
        DeliveryMode deliveryMode,
        String deliveryPoint,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
