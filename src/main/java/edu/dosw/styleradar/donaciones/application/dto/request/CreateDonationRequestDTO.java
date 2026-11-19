package edu.dosw.styleradar.donaciones.application.dto.request;

import java.time.LocalDateTime;

import edu.dosw.styleradar.donaciones.domain.model.DeliveryMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO para la solicitud de creación de una petición de donación hacia una fundación,
 * permitiendo resolver el donante desde el token de sesión JWT y recibir la propuesta
 * de entrega en el objeto estructurado {@code deliveryProposal} o en campos planos heredados.
 */
@Schema(description = "Request payload to create a new garment donation request with an initial delivery proposal")
public record CreateDonationRequestDTO(

        @Schema(description = "ID of the second-hand publication to donate", example = "1")
        @NotNull(message = "El ID de la publicación es obligatorio")
        Long publicationId,

        @Schema(description = "Optional donor user ID; resolved automatically from the authenticated JWT session token when omitted", example = "101", nullable = true)
        Long donorId,

        @Schema(description = "ID of the target foundation", example = "3")
        @NotNull(message = "El ID de la fundación es obligatorio")
        Long foundationId,

        @Schema(description = "Optional message from the donor to the foundation", example = "Deseo donar esta chaqueta para las campañas de invierno")
        @Size(max = 500, message = "El mensaje no puede exceder 500 caracteres")
        String message,

        @Schema(description = "Proposed delivery mode (legacy flat field; used when deliveryProposal is omitted)", example = "DROP_OFF_POINT", nullable = true)
        DeliveryMode deliveryMode,

        @Schema(description = "Proposed drop-off point or pickup address (legacy flat field; used when deliveryProposal is omitted)", example = "Calle 85 # 15-32, Chapinero, Bogotá", nullable = true)
        @Size(max = 255, message = "El punto de entrega no puede exceder 255 caracteres")
        String deliveryPoint,

        @Schema(description = "Start timestamp of the proposed window (legacy flat field; used when deliveryProposal is omitted)", example = "2026-12-15T09:00:00", nullable = true)
        LocalDateTime startTime,

        @Schema(description = "End timestamp of the proposed window (legacy flat field; used when deliveryProposal is omitted)", example = "2026-12-15T12:00:00", nullable = true)
        LocalDateTime endTime,

        @Schema(description = "Structured initial delivery proposal (mode, point/address, and time window); preferred over legacy flat fields")
        @Valid
        DeliveryProposalDTO deliveryProposal
) {

    /**
     * Constructor de compatibilidad hacia atrás para solicitudes con campos planos sin objeto anidado.
     */
    public CreateDonationRequestDTO(
            Long publicationId,
            Long donorId,
            Long foundationId,
            String message,
            DeliveryMode deliveryMode,
            String deliveryPoint,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        this(publicationId, donorId, foundationId, message, deliveryMode, deliveryPoint, startTime, endTime, null);
    }

    /**
     * Constructor conveniente para solicitudes con objeto {@code deliveryProposal} estructurado y donante inferido del JWT.
     */
    public CreateDonationRequestDTO(
            Long publicationId,
            Long foundationId,
            String message,
            DeliveryProposalDTO deliveryProposal
    ) {
        this(publicationId, null, foundationId, message, null, null, null, null, deliveryProposal);
    }

    public DeliveryMode resolvedDeliveryMode() {
        return deliveryProposal != null && deliveryProposal.deliveryMode() != null
                ? deliveryProposal.deliveryMode()
                : deliveryMode;
    }

    public String resolvedDeliveryPoint() {
        return deliveryProposal != null && deliveryProposal.deliveryPoint() != null
                ? deliveryProposal.deliveryPoint()
                : deliveryPoint;
    }

    public LocalDateTime resolvedStartTime() {
        return deliveryProposal != null && deliveryProposal.startTime() != null
                ? deliveryProposal.startTime()
                : startTime;
    }

    public LocalDateTime resolvedEndTime() {
        return deliveryProposal != null && deliveryProposal.endTime() != null
                ? deliveryProposal.endTime()
                : endTime;
    }

    @AssertTrue(message = "El modo de entrega propuesto es obligatorio")
    public boolean isDeliveryModePresent() {
        return resolvedDeliveryMode() != null;
    }

    @AssertTrue(message = "El punto de entrega o recogida propuesto es obligatorio")
    public boolean isDeliveryPointPresent() {
        String point = resolvedDeliveryPoint();
        return point != null && !point.trim().isEmpty();
    }

    @AssertTrue(message = "La fecha y hora de inicio y fin de la franja son obligatorias")
    public boolean isDeliveryWindowPresent() {
        return resolvedStartTime() != null && resolvedEndTime() != null;
    }
}
