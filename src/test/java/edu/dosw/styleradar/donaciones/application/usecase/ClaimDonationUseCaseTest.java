package edu.dosw.styleradar.donaciones.application.usecase;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import edu.dosw.styleradar.donaciones.domain.event.DonationRequestCreatedEvent;
import edu.dosw.styleradar.donaciones.domain.exception.ConcurrentDonationClaimException;
import edu.dosw.styleradar.donaciones.domain.exception.DomainConflictException;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidDonationRequestException;
import edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException;
import edu.dosw.styleradar.donaciones.domain.model.ConservationStatus;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequestStatus;
import edu.dosw.styleradar.donaciones.domain.model.Garment;
import edu.dosw.styleradar.donaciones.domain.model.GarmentType;
import edu.dosw.styleradar.donaciones.domain.model.LocalidadBogota;
import edu.dosw.styleradar.donaciones.domain.model.PublicationStatus;
import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;
import edu.dosw.styleradar.donaciones.domain.model.Size;
import edu.dosw.styleradar.donaciones.domain.model.Style;
import edu.dosw.styleradar.donaciones.domain.port.in.ClaimDonationCommand;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationRequestRepositoryPort;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicationRepositoryPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class ClaimDonationUseCaseTest {

    @Mock
    private PublicationRepositoryPort publicationRepositoryPort;

    @Mock
    private DonationRequestRepositoryPort donationRequestRepositoryPort;

    @Mock
    private DonationEventPublisherPort eventPublisherPort;

    @InjectMocks
    private ClaimDonationUseCaseImpl claimDonationUseCase;

    private SecondHandPublication donationPublication;

    @BeforeEach
    void setUp() {
        Garment garment = Garment.builder()
                .name("Chaqueta Térmica")
                .description("Para abrigo")
                .type(GarmentType.UPPER)
                .brand("North Face")
                .color("Negro")
                .style(Style.CASUAL)
                .build();

        donationPublication = SecondHandPublication.builder()
                .id(10L)
                .userId(101L) // Donante
                .garment(garment)
                .size(Size.L)
                .conservationStatus(ConservationStatus.GOOD_CONDITION)
                .price(BigDecimal.ZERO)
                .esDonacion(true)
                .localidad(LocalidadBogota.CHAPINERO)
                .status(PublicationStatus.AVAILABLE)
                .version(0L)
                .build();
    }

    @Test
    @DisplayName("Debe permitir a una fundación reclamar una donación de la vitrina con éxito")
    void shouldClaimDonationSuccessfully() {
        ClaimDonationCommand command = new ClaimDonationCommand(10L, 501L, "Reclamo para niños vulnerables");

        when(publicationRepositoryPort.findById(10L)).thenReturn(Optional.of(donationPublication));
        when(donationRequestRepositoryPort.existsActiveRequestForPublication(10L)).thenReturn(false);
        when(publicationRepositoryPort.save(any(SecondHandPublication.class))).thenAnswer(i -> i.getArgument(0));
        when(donationRequestRepositoryPort.save(any(DonationRequest.class))).thenAnswer(i -> {
            DonationRequest req = i.getArgument(0);
            req.setId(1L);
            return req;
        });

        DonationRequest result = claimDonationUseCase.claimDonation(command);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getPublicationId()).isEqualTo(10L);
        assertThat(result.getDonorId()).isEqualTo(101L);
        assertThat(result.getFoundationId()).isEqualTo(501L);
        assertThat(result.getStatus()).isEqualTo(DonationRequestStatus.APPROVED);
        assertThat(donationPublication.getStatus()).isEqualTo(PublicationStatus.IN_DONATION);

        verify(eventPublisherPort).publishEvent(any(DonationRequestCreatedEvent.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción si la prenda no está marcada para donación (es de reventa)")
    void shouldThrowWhenPublicationIsNotForDonation() {
        donationPublication.setEsDonacion(false);
        donationPublication.setPrice(new BigDecimal("50000.00"));

        ClaimDonationCommand command = new ClaimDonationCommand(10L, 501L, "Reclamo");
        when(publicationRepositoryPort.findById(10L)).thenReturn(Optional.of(donationPublication));

        assertThatThrownBy(() -> claimDonationUseCase.claimDonation(command))
                .isInstanceOf(InvalidDonationRequestException.class)
                .hasMessageContaining("no está marcada para donación benéfica");
    }

    @Test
    @DisplayName("Debe lanzar excepción si la donación no está en estado AVAILABLE en la vitrina")
    void shouldThrowWhenPublicationNotAvailable() {
        donationPublication.setStatus(PublicationStatus.IN_DONATION);

        ClaimDonationCommand command = new ClaimDonationCommand(10L, 501L, "Reclamo");
        when(publicationRepositoryPort.findById(10L)).thenReturn(Optional.of(donationPublication));

        assertThatThrownBy(() -> claimDonationUseCase.claimDonation(command))
                .isInstanceOf(DomainConflictException.class)
                .hasMessageContaining("ya fue reclamada o no está disponible");
    }

    @Test
    @DisplayName("Debe impedir que una fundación reclame su propia donación (regla anti-auto-donación)")
    void shouldThrowWhenFoundationClaimsOwnDonation() {
        donationPublication.setUserId(501L); // El donante es la misma fundación 501

        ClaimDonationCommand command = new ClaimDonationCommand(10L, 501L, "Auto-reclamo");
        when(publicationRepositoryPort.findById(10L)).thenReturn(Optional.of(donationPublication));

        assertThatThrownBy(() -> claimDonationUseCase.claimDonation(command))
                .isInstanceOf(InvalidDonationRequestException.class)
                .hasMessageContaining("no puede reclamar o aceptar su propia donación");
    }

    @Test
    @DisplayName("Debe capturar conflicto de concurrencia optimista y lanzar ConcurrentDonationClaimException (409)")
    void shouldThrowConcurrentDonationClaimExceptionOnOptimisticLock() {
        ClaimDonationCommand command = new ClaimDonationCommand(10L, 501L, "Reclamo concurrente");

        when(publicationRepositoryPort.findById(10L)).thenReturn(Optional.of(donationPublication));
        when(donationRequestRepositoryPort.existsActiveRequestForPublication(10L)).thenReturn(false);
        when(publicationRepositoryPort.save(any(SecondHandPublication.class)))
                .thenThrow(new OptimisticLockingFailureException("Fallo de bloqueo optimista"));

        assertThatThrownBy(() -> claimDonationUseCase.claimDonation(command))
                .isInstanceOf(ConcurrentDonationClaimException.class)
                .hasMessageContaining("ya fue reclamada concurrentemente por otra fundación");
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si la publicación no existe")
    void shouldThrowWhenPublicationNotFound() {
        ClaimDonationCommand command = new ClaimDonationCommand(999L, 501L, "Reclamo");
        when(publicationRepositoryPort.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> claimDonationUseCase.claimDonation(command))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No existe publicación");
    }

    @Test
    @DisplayName("Debe publicar evento solo tras afterCommit cuando hay sincronización transaccional activa")
    void shouldPublishEventOnlyAfterCommitWhenTransactionActive() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            ClaimDonationCommand command = new ClaimDonationCommand(10L, 501L, "Reclamo para niños");

            when(publicationRepositoryPort.findById(10L)).thenReturn(Optional.of(donationPublication));
            when(donationRequestRepositoryPort.existsActiveRequestForPublication(10L)).thenReturn(false);
            when(publicationRepositoryPort.save(any(SecondHandPublication.class))).thenAnswer(i -> i.getArgument(0));
            when(donationRequestRepositoryPort.save(any(DonationRequest.class))).thenAnswer(i -> {
                DonationRequest req = i.getArgument(0);
                req.setId(1L);
                return req;
            });

            DonationRequest result = claimDonationUseCase.claimDonation(command);

            assertThat(result).isNotNull();
            // Antes de ejecutar el commit: NO debe haberse publicado el evento en RabbitMQ
            verify(eventPublisherPort, never()).publishEvent(any(DonationRequestCreatedEvent.class));

            // Simular commit exitoso de la transacción en Spring
            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCommit();
            }

            // Tras el commit: Se publicó el evento con éxito
            verify(eventPublisherPort).publishEvent(any(DonationRequestCreatedEvent.class));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("No debe publicar evento si ocurre rollback y nunca se ejecuta afterCommit")
    void shouldNotPublishEventIfTransactionRollsBack() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            ClaimDonationCommand command = new ClaimDonationCommand(10L, 501L, "Reclamo para niños");

            when(publicationRepositoryPort.findById(10L)).thenReturn(Optional.of(donationPublication));
            when(donationRequestRepositoryPort.existsActiveRequestForPublication(10L)).thenReturn(false);
            when(publicationRepositoryPort.save(any(SecondHandPublication.class))).thenAnswer(i -> i.getArgument(0));
            when(donationRequestRepositoryPort.save(any(DonationRequest.class))).thenAnswer(i -> {
                DonationRequest req = i.getArgument(0);
                req.setId(1L);
                return req;
            });

            claimDonationUseCase.claimDonation(command);

            // Simular rollback (no se invoca afterCommit)
            // No se debe haber publicado ningún evento
            verify(eventPublisherPort, never()).publishEvent(any());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
