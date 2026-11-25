package edu.dosw.styleradar.donaciones.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.donaciones.domain.event.DonationRequestDecidedEvent;
import edu.dosw.styleradar.donaciones.domain.model.ConservationStatus;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequestStatus;
import edu.dosw.styleradar.donaciones.domain.model.Garment;
import edu.dosw.styleradar.donaciones.domain.model.GarmentType;
import edu.dosw.styleradar.donaciones.domain.model.PublicationStatus;
import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;
import edu.dosw.styleradar.donaciones.domain.model.Size;
import edu.dosw.styleradar.donaciones.domain.model.Style;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationRequestRepositoryPort;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicationRepositoryPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - HandleFoundationDeletedUseCaseImpl")
class HandleFoundationDeletedUseCaseTest {

    @Mock
    private DonationRequestRepositoryPort donationRequestRepositoryPort;

    @Mock
    private PublicationRepositoryPort publicationRepositoryPort;

    @Mock
    private DonationEventPublisherPort eventPublisherPort;

    @InjectMocks
    private HandleFoundationDeletedUseCaseImpl useCase;

    private SecondHandPublication donationPublication(Long id, PublicationStatus status) {
        return SecondHandPublication.builder()
                .id(id)
                .userId(101L)
                .garment(Garment.builder()
                        .name("Chaqueta")
                        .type(GarmentType.CHAQUETAS)
                        .brand("Levi's")
                        .color("Azul")
                        .style(Style.URBAN)
                        .build())
                .size(Size.M)
                .conservationStatus(ConservationStatus.LIKE_NEW)
                .price(BigDecimal.ZERO)
                .esDonacion(true)
                .status(status)
                .publishedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Debe cerrar solicitudes activas (PENDING/APPROVED/IN_PICKUP), revertir prendas IN_DONATION a AVAILABLE y emitir eventos")
    void shouldCancelAllActiveRequestsAndRevertPublications() {
        DonationRequest pending = DonationRequest.builder()
                .id(1L).publicationId(10L).donorId(101L).foundationId(3L)
                .status(DonationRequestStatus.PENDING).requestedAt(LocalDateTime.now()).build();
        DonationRequest approved = DonationRequest.builder()
                .id(2L).publicationId(11L).donorId(102L).foundationId(3L)
                .status(DonationRequestStatus.APPROVED).requestedAt(LocalDateTime.now()).build();
        DonationRequest inPickup = DonationRequest.builder()
                .id(3L).publicationId(null).donorId(103L).foundationId(3L)
                .status(DonationRequestStatus.IN_PICKUP).requestedAt(LocalDateTime.now()).build();

        SecondHandPublication pubInDonation = donationPublication(10L, PublicationStatus.IN_DONATION);
        SecondHandPublication pubAlreadyAvailable = donationPublication(11L, PublicationStatus.AVAILABLE);

        when(donationRequestRepositoryPort.findActiveByFoundationId(3L))
                .thenReturn(List.of(pending, approved, inPickup));
        when(publicationRepositoryPort.findById(10L)).thenReturn(Optional.of(pubInDonation));
        when(publicationRepositoryPort.findById(11L)).thenReturn(Optional.of(pubAlreadyAvailable));
        when(donationRequestRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int cancelled = useCase.handleFoundationDeleted(3L);

        assertThat(cancelled).isEqualTo(3);
        assertThat(pending.getStatus()).isEqualTo(DonationRequestStatus.REJECTED);
        assertThat(approved.getStatus()).isEqualTo(DonationRequestStatus.REJECTED);
        assertThat(inPickup.getStatus()).isEqualTo(DonationRequestStatus.REJECTED);
        assertThat(pending.getRejectionReason()).isEqualTo(HandleFoundationDeletedUseCaseImpl.CANCELLATION_REASON);
        assertThat(pubInDonation.getStatus()).isEqualTo(PublicationStatus.AVAILABLE);
        assertThat(pubAlreadyAvailable.getStatus()).isEqualTo(PublicationStatus.AVAILABLE);

        verify(publicationRepositoryPort, times(1)).save(pubInDonation);
        verify(publicationRepositoryPort, never()).save(pubAlreadyAvailable);
        verify(donationRequestRepositoryPort, times(3)).save(any());

        ArgumentCaptor<DonationRequestDecidedEvent> eventCaptor = ArgumentCaptor.forClass(DonationRequestDecidedEvent.class);
        verify(eventPublisherPort, times(3)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .extracting(DonationRequestDecidedEvent::requestId, DonationRequestDecidedEvent::approved)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1L, false),
                        org.assertj.core.groups.Tuple.tuple(2L, false),
                        org.assertj.core.groups.Tuple.tuple(3L, false)
                );
    }

    @Test
    @DisplayName("Debe ser idempotente cuando no hay solicitudes activas o foundationId es nulo")
    void shouldBeIdempotentWhenNoActiveRequestsOrNullFoundationId() {
        assertThat(useCase.handleFoundationDeleted(null)).isZero();
        verifyNoInteractions(donationRequestRepositoryPort, publicationRepositoryPort, eventPublisherPort);

        when(donationRequestRepositoryPort.findActiveByFoundationId(99L)).thenReturn(List.of());
        assertThat(useCase.handleFoundationDeleted(99L)).isZero();
        verifyNoInteractions(publicationRepositoryPort, eventPublisherPort);
    }
}
