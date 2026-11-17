package edu.dosw.styleradar.donaciones.domain.model;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.dosw.styleradar.donaciones.domain.exception.InvalidDonationRequestException;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidStateTransitionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para la entidad y máquina de estados DonationRequest.
 */
@DisplayName("Pruebas Unitarias - Entidad DonationRequest")
class DonationRequestTest {

    private DonationRequest.DonationRequestBuilder validProposalBuilder() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        return DonationRequest.builder()
                .publicationId(100L)
                .donorId(1L)
                .foundationId(50L)
                .message("Ropa en perfecto estado para niños")
                .deliveryMode(DeliveryMode.DROP_OFF_POINT)
                .deliveryPoint("  Calle 85 # 15-32, Bogotá  ")
                .pickupStartTime(start)
                .pickupEndTime(start.plusHours(2));
    }

    @Test
    @DisplayName("Debe iniciar solicitud con estado PENDING, propuesta de entrega e invariantes válidas")
    void shouldInitiateDonationRequestSuccessfully() {
        DonationRequest request = validProposalBuilder().build();

        request.initiate();

        assertEquals(DonationRequestStatus.PENDING, request.getStatus());
        assertEquals(DeliveryMode.DROP_OFF_POINT, request.getDeliveryMode());
        assertEquals("Calle 85 # 15-32, Bogotá", request.getDeliveryPoint());
        assertNotNull(request.getPickupStartTime());
        assertNotNull(request.getPickupEndTime());
        assertNotNull(request.getRequestedAt());
        assertTrue(request.getStatus().isInProgress());
        assertFalse(request.getStatus().isFinished());
    }

    @Test
    @DisplayName("Debe fallar al iniciar solicitud cuando faltan identificadores o propuesta de entrega")
    void shouldFailWhenInitialInvariantsMissing() {
        LocalDateTime futureStart = LocalDateTime.now().plusDays(1);
        LocalDateTime futureEnd = futureStart.plusHours(2);

        DonationRequest request = DonationRequest.builder().build();
        assertThrows(InvalidDonationRequestException.class, request::initiate);

        request.setPublicationId(10L);
        assertThrows(InvalidDonationRequestException.class, request::initiate);

        request.setDonorId(2L);
        assertThrows(InvalidDonationRequestException.class, request::initiate);

        request.setFoundationId(3L);
        assertThrows(InvalidDonationRequestException.class, request::initiate);

        request.setDeliveryMode(DeliveryMode.HOME_PICKUP);
        assertThrows(InvalidDonationRequestException.class, request::initiate);

        request.setDeliveryPoint(" ");
        assertThrows(InvalidDonationRequestException.class, request::initiate);

        request.setDeliveryPoint("Carrera 7 # 45-10, Bogotá");
        request.setPickupStartTime(futureStart);
        assertThrows(InvalidDonationRequestException.class, request::initiate);

        request.setPickupEndTime(futureStart.minusHours(1));
        assertThrows(InvalidDonationRequestException.class, request::initiate);

        request.setPickupStartTime(LocalDateTime.now().minusDays(1));
        request.setPickupEndTime(futureEnd);
        assertThrows(InvalidDonationRequestException.class, request::initiate);
    }

    @Test
    @DisplayName("Debe aprobar y rechazar solicitud en estado PENDING")
    void shouldApproveAndRejectRequest() {
        DonationRequest requestToApprove = validProposalBuilder()
                .publicationId(10L)
                .donorId(2L)
                .foundationId(3L)
                .build();
        requestToApprove.create();
        requestToApprove.approve();

        assertEquals(DonationRequestStatus.APPROVED, requestToApprove.getStatus());
        assertNotNull(requestToApprove.getDecidedAt());
        assertTrue(requestToApprove.getStatus().isInProgress());

        // Intentar aprobar nuevamente debe lanzar excepción
        assertThrows(InvalidStateTransitionException.class, requestToApprove::accept);

        // Rechazo
        DonationRequest requestToReject = validProposalBuilder()
                .publicationId(11L)
                .donorId(2L)
                .foundationId(3L)
                .build();
        requestToReject.initiate();

        assertThrows(InvalidDonationRequestException.class, () -> requestToReject.reject("   "));
        assertThrows(InvalidDonationRequestException.class, () -> requestToReject.reject(null));

        requestToReject.reject("No recibimos calzado en este momento");
        assertEquals(DonationRequestStatus.REJECTED, requestToReject.getStatus());
        assertEquals("No recibimos calzado en este momento", requestToReject.getRejectionReason());
        assertNotNull(requestToReject.getDecidedAt());
        assertTrue(requestToReject.getStatus().isFinished());
        assertFalse(requestToReject.getStatus().isInProgress());

        // No se puede rechazar si ya no está PENDING
        assertThrows(InvalidStateTransitionException.class, () -> requestToReject.reject("Otro motivo"));
    }

    @Test
    @DisplayName("Debe programar recogida cuando la solicitud está aprobada")
    void shouldSchedulePickupCorrectly() {
        DonationRequest request = validProposalBuilder()
                .publicationId(10L)
                .donorId(2L)
                .foundationId(3L)
                .build();
        request.initiate();

        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);

        // No puede programar si aún está PENDING
        assertThrows(InvalidStateTransitionException.class,
                () -> request.schedulePickup("Sede Norte", start, end));

        request.approve();

        // Validaciones de franja y punto
        assertThrows(InvalidDonationRequestException.class,
                () -> request.schedulePickup(" ", start, end));
        assertThrows(InvalidDonationRequestException.class,
                () -> request.schedulePickup("Sede Norte", null, end));
        LocalDateTime pastStart = LocalDateTime.now().minusDays(1);
        assertThrows(InvalidDonationRequestException.class,
                () -> request.schedulePickup("Sede Norte", pastStart, end)); // en el pasado
        assertThrows(InvalidDonationRequestException.class,
                () -> request.schedulePickup("Sede Norte", end, start)); // fin antes de inicio

        request.schedulePickup("Carrera 7 # 120-10, Bogotá", start, end);

        assertEquals(DonationRequestStatus.IN_PICKUP, request.getStatus());
        assertEquals("Carrera 7 # 120-10, Bogotá", request.getDeliveryPoint());
        assertEquals(start, request.getPickupStartTime());
        assertEquals(end, request.getPickupEndTime());
    }

    @Test
    @DisplayName("Debe confirmar entrega cuando la solicitud está en recogida")
    void shouldConfirmDeliveryCorrectly() {
        DonationRequest request = validProposalBuilder()
                .publicationId(10L)
                .donorId(2L)
                .foundationId(3L)
                .build();
        request.initiate();

        // No puede confirmar entrega en estado PENDING
        assertThrows(InvalidStateTransitionException.class, request::confirmDelivery);

        request.approve();
        // No puede confirmar entrega en estado APPROVED
        assertThrows(InvalidStateTransitionException.class, request::confirmDelivery);

        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);
        request.schedulePickup("Punto Acopio Central", start, end);

        request.confirmDelivery();

        assertEquals(DonationRequestStatus.DELIVERED, request.getStatus());
        assertNotNull(request.getReceivedAt());
        assertTrue(request.getStatus().isFinished());

        // No puede volver a confirmar si ya está DELIVERED
        assertThrows(InvalidStateTransitionException.class, request::confirmDelivery);
    }
}
