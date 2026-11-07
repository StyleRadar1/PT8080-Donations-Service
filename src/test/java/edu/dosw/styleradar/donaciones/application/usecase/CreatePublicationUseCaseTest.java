package edu.dosw.styleradar.donaciones.application.usecase;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.donaciones.domain.event.PublicationCreatedEvent;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidPublicationException;
import edu.dosw.styleradar.donaciones.domain.model.ConservationStatus;
import edu.dosw.styleradar.donaciones.domain.model.Garment;
import edu.dosw.styleradar.donaciones.domain.model.GarmentType;
import edu.dosw.styleradar.donaciones.domain.model.PublicationStatus;
import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;
import edu.dosw.styleradar.donaciones.domain.model.Size;
import edu.dosw.styleradar.donaciones.domain.model.Style;
import edu.dosw.styleradar.donaciones.domain.port.in.CreatePublicationCommand;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicationRepositoryPort;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para el caso de uso CreatePublicationUseCaseImpl.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - CreatePublicationUseCaseImpl")
class CreatePublicationUseCaseTest {

    @Mock
    private PublicationRepositoryPort publicationRepositoryPort;

    @Mock
    private DonationEventPublisherPort eventPublisherPort;

    @InjectMocks
    private CreatePublicationUseCaseImpl createPublicationUseCase;

    private Garment garment;

    @BeforeEach
    void setUp() {
        garment = Garment.builder()
                .id(1L)
                .name("Buzo Oversize")
                .type(GarmentType.UPPER)
                .brand("H&M")
                .color("Gris")
                .style(Style.URBAN)
                .build();
    }

    @Test
    @DisplayName("Debe crear la publicación exitosamente y publicar evento de dominio")
    void shouldCreatePublicationSuccessfully() {
        CreatePublicationCommand command = new CreatePublicationCommand(
                10L,
                garment,
                Size.L,
                ConservationStatus.LIKE_NEW,
                BigDecimal.valueOf(65000.0),
                List.of("https://cdn.style.com/foto1.jpg")
        );

        when(publicationRepositoryPort.save(any(SecondHandPublication.class))).thenAnswer(inv -> {
            SecondHandPublication p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });

        SecondHandPublication result = createPublicationUseCase.create(command);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(PublicationStatus.AVAILABLE, result.getStatus());
        assertEquals(BigDecimal.valueOf(65000.0), result.getPrice());

        verify(publicationRepositoryPort, times(1)).save(any(SecondHandPublication.class));
        verify(eventPublisherPort, times(1)).publishEvent(any(PublicationCreatedEvent.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el comando contiene datos inválidos")
    void shouldFailWhenCommandDataIsInvalid() {
        CreatePublicationCommand invalidCommand = new CreatePublicationCommand(
                10L,
                garment,
                Size.L,
                ConservationStatus.LIKE_NEW,
                BigDecimal.valueOf(-50.0), // precio negativo
                List.of()
        );

        assertThrows(InvalidPublicationException.class, () -> createPublicationUseCase.create(invalidCommand));

        verify(publicationRepositoryPort, never()).save(any());
        verify(eventPublisherPort, never()).publishEvent(any());
    }
}
