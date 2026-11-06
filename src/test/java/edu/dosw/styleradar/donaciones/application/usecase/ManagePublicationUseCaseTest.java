package edu.dosw.styleradar.donaciones.application.usecase;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.donaciones.domain.event.PublicationSoldEvent;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidPublicationException;
import edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException;
import edu.dosw.styleradar.donaciones.domain.model.ConservationStatus;
import edu.dosw.styleradar.donaciones.domain.model.Garment;
import edu.dosw.styleradar.donaciones.domain.model.GarmentType;
import edu.dosw.styleradar.donaciones.domain.model.PublicationStatus;
import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;
import edu.dosw.styleradar.donaciones.domain.model.Size;
import edu.dosw.styleradar.donaciones.domain.port.in.EditPublicationCommand;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicationRepositoryPort;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para el caso de uso ManagePublicationUseCaseImpl.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - ManagePublicationUseCaseImpl")
class ManagePublicationUseCaseTest {

    @Mock
    private PublicationRepositoryPort publicationRepositoryPort;

    @Mock
    private DonationEventPublisherPort eventPublisherPort;

    @InjectMocks
    private ManagePublicationUseCaseImpl managePublicationUseCase;

    private SecondHandPublication publication;
    private Garment garment;

    @BeforeEach
    void setUp() {
        garment = Garment.builder()
                .id(1L)
                .name("Vestido Floreado")
                .type(GarmentType.DRESS)
                .build();

        publication = SecondHandPublication.builder()
                .id(50L)
                .userId(10L)
                .garment(garment)
                .size(Size.S)
                .conservationStatus(ConservationStatus.LIKE_NEW)
                .price(BigDecimal.valueOf(80000.0))
                .status(PublicationStatus.AVAILABLE)
                .build();
    }

    @Test
    @DisplayName("Debe editar publicación exitosamente")
    void shouldEditPublicationSuccessfully() {
        EditPublicationCommand command = new EditPublicationCommand(
                10L,
                50L,
                garment,
                Size.M,
                ConservationStatus.GOOD_CONDITION,
                BigDecimal.valueOf(75000.0),
                List.of("nueva_foto.jpg")
        );

        when(publicationRepositoryPort.findById(50L)).thenReturn(Optional.of(publication));
        when(publicationRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SecondHandPublication result = managePublicationUseCase.edit(command);

        assertEquals(Size.M, result.getSize());
        assertEquals(BigDecimal.valueOf(75000.0), result.getPrice());
        verify(publicationRepositoryPort).save(any());
    }

    @Test
    @DisplayName("Debe rechazar edición si el usuario no es el propietario")
    void shouldRejectEditWhenNotOwner() {
        EditPublicationCommand command = new EditPublicationCommand(
                999L, // usuario distinto
                50L,
                garment,
                Size.M,
                ConservationStatus.GOOD_CONDITION,
                BigDecimal.valueOf(75000.0),
                List.of()
        );

        when(publicationRepositoryPort.findById(50L)).thenReturn(Optional.of(publication));

        assertThrows(InvalidPublicationException.class, () -> managePublicationUseCase.edit(command));
        verify(publicationRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si la publicación a editar no existe")
    void shouldFailWhenPublicationToEditNotFound() {
        EditPublicationCommand command = new EditPublicationCommand(
                10L, 999L, garment, Size.M, ConservationStatus.GOOD_CONDITION, BigDecimal.valueOf(75000.0), List.of()
        );

        when(publicationRepositoryPort.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> managePublicationUseCase.edit(command));
    }

    @Test
    @DisplayName("Debe marcar como vendida y publicar evento")
    void shouldMarkAsSoldAndPublishEvent() {
        when(publicationRepositoryPort.findById(50L)).thenReturn(Optional.of(publication));
        when(publicationRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SecondHandPublication result = managePublicationUseCase.markAsSold(10L, 50L);

        assertEquals(PublicationStatus.SOLD, result.getStatus());
        verify(eventPublisherPort).publishEvent(any(PublicationSoldEvent.class));
    }

    @Test
    @DisplayName("Debe retirar publicación correctamente")
    void shouldRetirePublication() {
        when(publicationRepositoryPort.findById(50L)).thenReturn(Optional.of(publication));
        when(publicationRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        managePublicationUseCase.retire(10L, 50L);

        assertEquals(PublicationStatus.RETIRED, publication.getStatus());
        verify(publicationRepositoryPort).save(publication);
    }

    @Test
    @DisplayName("Debe destinar publicación a donación correctamente")
    void shouldMarkForDonation() {
        when(publicationRepositoryPort.findById(50L)).thenReturn(Optional.of(publication));
        when(publicationRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SecondHandPublication result = managePublicationUseCase.markForDonation(10L, 50L);

        assertEquals(PublicationStatus.IN_DONATION, result.getStatus());
        verify(publicationRepositoryPort).save(publication);
    }
}
