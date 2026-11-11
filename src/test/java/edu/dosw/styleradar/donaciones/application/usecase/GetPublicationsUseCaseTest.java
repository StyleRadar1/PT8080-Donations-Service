package edu.dosw.styleradar.donaciones.application.usecase;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException;
import edu.dosw.styleradar.donaciones.domain.model.ConservationStatus;
import edu.dosw.styleradar.donaciones.domain.model.Garment;
import edu.dosw.styleradar.donaciones.domain.model.GarmentType;
import edu.dosw.styleradar.donaciones.domain.model.PublicationSearchCriteria;
import edu.dosw.styleradar.donaciones.domain.model.PublicationStatus;
import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;
import edu.dosw.styleradar.donaciones.domain.model.Size;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicationRepositoryPort;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para el caso de uso GetPublicationsUseCaseImpl.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - GetPublicationsUseCaseImpl")
class GetPublicationsUseCaseTest {

    @Mock
    private PublicationRepositoryPort publicationRepositoryPort;

    @InjectMocks
    private GetPublicationsUseCaseImpl getPublicationsUseCase;

    @Test
    @DisplayName("Debe retornar publicación cuando existe por ID")
    void shouldReturnPublicationById() {
        SecondHandPublication pub = SecondHandPublication.builder()
                .id(1L)
                .userId(5L)
                .garment(Garment.builder().name("Camisa").type(GarmentType.UPPER).build())
                .size(Size.M)
                .conservationStatus(ConservationStatus.GOOD_CONDITION)
                .price(BigDecimal.valueOf(25000.0))
                .status(PublicationStatus.AVAILABLE)
                .build();

        when(publicationRepositoryPort.findById(1L)).thenReturn(Optional.of(pub));

        SecondHandPublication result = getPublicationsUseCase.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Camisa", result.getGarment().getName());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException cuando no existe la publicación")
    void shouldThrowExceptionWhenNotFound() {
        when(publicationRepositoryPort.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> getPublicationsUseCase.getById(999L));
    }

    @Test
    @DisplayName("Debe retornar lista de publicaciones por usuario")
    void shouldReturnPublicationsByUser() {
        SecondHandPublication pub1 = SecondHandPublication.builder().id(1L).userId(5L).build();
        SecondHandPublication pub2 = SecondHandPublication.builder().id(2L).userId(5L).build();

        when(publicationRepositoryPort.findByUserId(5L)).thenReturn(List.of(pub1, pub2));

        List<SecondHandPublication> result = getPublicationsUseCase.getByUserId(5L);

        assertEquals(2, result.size());
        verify(publicationRepositoryPort, times(1)).findByUserId(5L);
    }

    @Test
    @DisplayName("Debe retornar publicaciones filtradas por criterios de búsqueda")
    void shouldSearchByCriteria() {
        PublicationSearchCriteria criteria = PublicationSearchCriteria.builder()
                .type(GarmentType.UPPER)
                .size(Size.M)
                .build();

        SecondHandPublication pub = SecondHandPublication.builder().id(1L).build();
        when(publicationRepositoryPort.findByCriteria(criteria)).thenReturn(List.of(pub));

        List<SecondHandPublication> result = getPublicationsUseCase.search(criteria);

        assertEquals(1, result.size());
        verify(publicationRepositoryPort, times(1)).findByCriteria(criteria);
    }
}
