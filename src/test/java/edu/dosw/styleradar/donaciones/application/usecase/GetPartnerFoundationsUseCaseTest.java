package edu.dosw.styleradar.donaciones.application.usecase;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.donaciones.domain.exception.InvalidDonationRequestException;
import edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException;
import edu.dosw.styleradar.donaciones.domain.model.FoundationSummary;
import edu.dosw.styleradar.donaciones.domain.port.out.FoundationCatalogPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - GetPartnerFoundationsUseCaseImpl")
class GetPartnerFoundationsUseCaseTest {

    @Mock
    private FoundationCatalogPort foundationCatalogPort;

    @InjectMocks
    private GetPartnerFoundationsUseCaseImpl useCase;

    private FoundationSummary foundation(Long id, String nombre, List<String> categorias, boolean activa) {
        return new FoundationSummary(
                id,
                nombre,
                "Chapinero",
                "Descripción",
                "Calle 85 # 15-32",
                "+57 601 742 8899",
                "contacto@fundacion.org",
                categorias,
                "Niñez vulnerable",
                "08:00-17:00",
                "Ropa limpia",
                activa
        );
    }

    @Test
    @DisplayName("Debe listar fundaciones activas ordenadas alfabéticamente excluyendo inactivas")
    void shouldReturnActiveFoundationsSortedByName() {
        when(foundationCatalogPort.findAllActiveFoundations()).thenReturn(List.of(
                foundation(2L, "Fundación Zapatos Felices", List.of("CALZADO"), true),
                foundation(3L, "Fundación Inactiva", List.of("SUPERIOR"), false),
                foundation(1L, "Fundación Abrigo Solidario", List.of("SUPERIOR", "INFERIOR"), true)
        ));

        List<FoundationSummary> result = useCase.getActiveFoundations(null);

        assertThat(result).extracting(FoundationSummary::id).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("Debe normalizar valores GarmentType (UPPER/FOOTWEAR/ACCESSORY) al filtrar por categoría")
    void shouldNormalizeGarmentTypeFilter() {
        when(foundationCatalogPort.findAllActiveFoundations()).thenReturn(List.of(
                foundation(1L, "Abrigo Solidario", List.of("SUPERIOR", "INFERIOR"), true),
                foundation(2L, "Pasos Solidarios", List.of("CALZADO"), true)
        ));

        List<FoundationSummary> upperResult = useCase.getActiveFoundations("UPPER");
        List<FoundationSummary> calzadoResult = useCase.getActiveFoundations("CALZADO");

        assertThat(upperResult).extracting(FoundationSummary::id).containsExactly(1L);
        assertThat(calzadoResult).extracting(FoundationSummary::id).containsExactly(2L);
    }

    @Test
    @DisplayName("Debe retornar el detalle si la fundación existe y está activa")
    void shouldReturnActiveFoundationById() {
        FoundationSummary active = foundation(3L, "Fundación Abrigo Solidario", List.of("SUPERIOR"), true);
        when(foundationCatalogPort.findFoundationById(3L)).thenReturn(Optional.of(active));

        FoundationSummary result = useCase.getActiveFoundationById(3L);

        assertThat(result.id()).isEqualTo(3L);
        assertThat(result.activa()).isTrue();
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si la fundación no existe en ms-tiendas")
    void shouldThrowNotFoundWhenFoundationMissing() {
        when(foundationCatalogPort.findFoundationById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getActiveFoundationById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Debe lanzar InvalidDonationRequestException si la fundación está inactiva")
    void shouldThrowBadRequestWhenFoundationInactive() {
        FoundationSummary inactive = foundation(3L, "Fundación Cerrada", List.of("SUPERIOR"), false);
        when(foundationCatalogPort.findFoundationById(3L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> useCase.getActiveFoundationById(3L))
                .isInstanceOf(InvalidDonationRequestException.class);
    }

    @Test
    @DisplayName("Debe encontrar la fundación activa asociada al representanteId del usuario")
    void shouldGetFoundationByRepresentativeId() {
        FoundationSummary matched = new FoundationSummary(
                3L, "Abrigo Solidario", "Chapinero", "Desc", "Dir", "Tel",
                "contacto@abrigosolidario.org", List.of("SUPERIOR"), "Niñez", "08:00-17:00", "Política", true, 24L
        );
        when(foundationCatalogPort.findAllActiveFoundations()).thenReturn(List.of(matched));

        FoundationSummary found = useCase.getFoundationByRepresentativeId(24L);

        assertThat(found.id()).isEqualTo(3L);
        assertThat(found.representanteId()).isEqualTo(24L);
    }
}
