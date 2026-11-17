package edu.dosw.styleradar.donaciones.application.usecase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.donaciones.domain.event.DonationRequestCreatedEvent;
import edu.dosw.styleradar.donaciones.domain.exception.DomainConflictException;
import edu.dosw.styleradar.donaciones.domain.exception.ExternalServiceUnavailableException;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidDonationRequestException;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidPublicationException;
import edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException;
import edu.dosw.styleradar.donaciones.domain.model.DeliveryMode;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequestStatus;
import edu.dosw.styleradar.donaciones.domain.model.PublicationStatus;
import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;
import edu.dosw.styleradar.donaciones.domain.port.in.CreateDonationRequestCommand;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationRequestRepositoryPort;
import edu.dosw.styleradar.donaciones.domain.port.out.FoundationCatalogPort;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicationRepositoryPort;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para el caso de uso CreateDonationRequestUseCaseImpl.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - CreateDonationRequestUseCaseImpl")
class CreateDonationRequestUseCaseTest {

    @Mock
    private DonationRequestRepositoryPort donationRequestRepositoryPort;

    @Mock
    private PublicationRepositoryPort publicationRepositoryPort;

    @Mock
    private DonationEventPublisherPort eventPublisherPort;

    @Mock
    private FoundationCatalogPort foundationCatalogPort;

    @InjectMocks
    private CreateDonationRequestUseCaseImpl createDonationRequestUseCase;

    private SecondHandPublication publicationInDonation;
    private LocalDateTime futureStart;
    private LocalDateTime futureEnd;

    @BeforeEach
    void setUp() {
        publicationInDonation = SecondHandPublication.builder()
                .id(100L)
                .userId(5L)
                .status(PublicationStatus.IN_DONATION)
                .build();
        futureStart = LocalDateTime.now().plusDays(1);
        futureEnd = futureStart.plusHours(3);
    }

    private CreateDonationRequestCommand buildCommand(Long donorId, Long publicationId, Long foundationId, String msg) {
        return new CreateDonationRequestCommand(
                donorId, publicationId, foundationId, msg,
                DeliveryMode.DROP_OFF_POINT, "Calle 85 # 15-32, Chapinero, Bogotá", futureStart, futureEnd
        );
    }

    @Test
    @DisplayName("Debe crear solicitud de donación exitosamente con propuesta de entrega y publicar evento")
    void shouldCreateDonationRequestSuccessfully() {
        CreateDonationRequestCommand command = buildCommand(
                5L, 100L, 20L, "Deseo donar esta prenda a su fundación"
        );

        when(publicationRepositoryPort.findById(100L)).thenReturn(Optional.of(publicationInDonation));
        when(donationRequestRepositoryPort.existsActiveRequestForPublication(100L)).thenReturn(false);
        when(donationRequestRepositoryPort.save(any(DonationRequest.class))).thenAnswer(inv -> {
            DonationRequest r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        DonationRequest result = createDonationRequestUseCase.create(command);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(DonationRequestStatus.PENDING, result.getStatus());
        assertEquals(DeliveryMode.DROP_OFF_POINT, result.getDeliveryMode());
        assertEquals("Calle 85 # 15-32, Chapinero, Bogotá", result.getDeliveryPoint());
        assertEquals(futureStart, result.getPickupStartTime());
        assertEquals(futureEnd, result.getPickupEndTime());

        verify(foundationCatalogPort).validateActiveFoundation(20L);
        verify(donationRequestRepositoryPort).save(any());
        verify(eventPublisherPort).publishEvent(any(DonationRequestCreatedEvent.class));
    }

    @Test
    @DisplayName("Debe fallar si la publicación no existe")
    void shouldFailWhenPublicationDoesNotExist() {
        CreateDonationRequestCommand command = buildCommand(5L, 999L, 20L, "Mensaje");
        when(publicationRepositoryPort.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> createDonationRequestUseCase.create(command));
    }

    @Test
    @DisplayName("Debe fallar si el donante no es el dueño de la publicación")
    void shouldFailWhenDonorIsNotOwner() {
        CreateDonationRequestCommand command = buildCommand(999L, 100L, 20L, "Mensaje");
        when(publicationRepositoryPort.findById(100L)).thenReturn(Optional.of(publicationInDonation));

        assertThrows(InvalidPublicationException.class, () -> createDonationRequestUseCase.create(command));
    }

    @Test
    @DisplayName("Debe fallar si la publicación no está marcada en IN_DONATION")
    void shouldFailWhenPublicationNotInDonationState() {
        publicationInDonation.setStatus(PublicationStatus.AVAILABLE);
        CreateDonationRequestCommand command = buildCommand(5L, 100L, 20L, "Mensaje");
        when(publicationRepositoryPort.findById(100L)).thenReturn(Optional.of(publicationInDonation));

        assertThrows(InvalidDonationRequestException.class, () -> createDonationRequestUseCase.create(command));
    }

    @Test
    @DisplayName("Debe fallar si ya existe una solicitud en curso para esa publicación")
    void shouldFailWhenActiveRequestAlreadyExists() {
        CreateDonationRequestCommand command = buildCommand(5L, 100L, 20L, "Mensaje");
        when(publicationRepositoryPort.findById(100L)).thenReturn(Optional.of(publicationInDonation));
        when(donationRequestRepositoryPort.existsActiveRequestForPublication(100L)).thenReturn(true);

        assertThrows(DomainConflictException.class, () -> createDonationRequestUseCase.create(command));
    }

    @Test
    @DisplayName("Debe consultar solicitudes de donante y de fundación")
    void shouldConsultRequests() {
        when(donationRequestRepositoryPort.findByDonorId(5L)).thenReturn(List.of(new DonationRequest()));
        when(donationRequestRepositoryPort.findByFoundationId(20L)).thenReturn(List.of(new DonationRequest()));
        when(donationRequestRepositoryPort.findByFoundationIdAndStatus(20L, DonationRequestStatus.PENDING))
                .thenReturn(List.of(new DonationRequest()));

        assertEquals(1, createDonationRequestUseCase.getByDonorId(5L).size());
        assertEquals(1, createDonationRequestUseCase.getByFoundationId(20L, null).size());
        assertEquals(1, createDonationRequestUseCase.getByFoundationId(20L, DonationRequestStatus.PENDING).size());
    }

    @Test
    @DisplayName("Debe obtener solicitud por ID o lanzar excepción")
    void shouldGetByIdOrThrow() {
        DonationRequest req = DonationRequest.builder().id(1L).build();
        when(donationRequestRepositoryPort.findById(1L)).thenReturn(Optional.of(req));
        when(donationRequestRepositoryPort.findById(999L)).thenReturn(Optional.empty());

        assertNotNull(createDonationRequestUseCase.getById(1L));
        assertThrows(ResourceNotFoundException.class, () -> createDonationRequestUseCase.getById(999L));
    }

    @Test
    @DisplayName("Debe rechazar la solicitud si la fundación no existe o está inactiva en ms-tiendas")
    void shouldFailWhenFoundationIsInvalidOrInactive() {
        CreateDonationRequestCommand command = buildCommand(5L, 100L, 999L, "Mensaje");
        when(publicationRepositoryPort.findById(100L)).thenReturn(Optional.of(publicationInDonation));
        when(donationRequestRepositoryPort.existsActiveRequestForPublication(100L)).thenReturn(false);
        doThrow(new InvalidDonationRequestException("La fundación no está activa"))
                .when(foundationCatalogPort).validateActiveFoundation(999L);

        assertThrows(InvalidDonationRequestException.class, () -> createDonationRequestUseCase.create(command));
        verify(donationRequestRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Debe propagar error 503 cuando ms-tiendas no está disponible")
    void shouldPropagateExternalServiceUnavailable() {
        CreateDonationRequestCommand command = buildCommand(5L, 100L, 20L, "Mensaje");
        when(publicationRepositoryPort.findById(100L)).thenReturn(Optional.of(publicationInDonation));
        when(donationRequestRepositoryPort.existsActiveRequestForPublication(100L)).thenReturn(false);
        doThrow(new ExternalServiceUnavailableException("ms-tiendas no disponible"))
                .when(foundationCatalogPort).validateActiveFoundation(20L);

        assertThrows(ExternalServiceUnavailableException.class, () -> createDonationRequestUseCase.create(command));
        verify(donationRequestRepositoryPort, never()).save(any());
    }
}
