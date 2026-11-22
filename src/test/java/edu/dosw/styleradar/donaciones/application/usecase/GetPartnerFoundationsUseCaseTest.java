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
        List<FoundationSummary> camisasResult = useCase.getActiveFoundations("Camisas");
        List<FoundationSummary> sueteresResult = useCase.getActiveFoundations("Suéteres");
        List<FoundationSummary> pantalonesResult = useCase.getActiveFoundations("Pantalones");
        List<FoundationSummary> calzadoResult = useCase.getActiveFoundations("CALZADO");

        assertThat(upperResult).extracting(FoundationSummary::id).containsExactly(1L);
        assertThat(camisasResult).extracting(FoundationSummary::id).containsExactly(1L);
        assertThat(sueteresResult).extracting(FoundationSummary::id).containsExactly(1L);
        assertThat(pantalonesResult).extracting(FoundationSummary::id).containsExactly(1L);
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
    @DisplayName("Debe encontrar la fundación activa únicamente por representanteId y rechazar coincidencia numérica con id")
    void shouldGetFoundationByRepresentativeId() {
        FoundationSummary matched = new FoundationSummary(
                3L, "Abrigo Solidario", "Chapinero", "Desc", "Dir", "Tel",
                "contacto@abrigosolidario.org", List.of("SUPERIOR"), "Niñez", "08:00-17:00", "Política", true, 24L
        );
        when(foundationCatalogPort.findAllActiveFoundations()).thenReturn(List.of(matched));

        FoundationSummary found = useCase.getFoundationByRepresentativeId(24L);

        assertThat(found.id()).isEqualTo(3L);
        assertThat(found.representanteId()).isEqualTo(24L);

        // Coincidencia numérica con foundation.id (3L) sin ser representanteId (24L) debe ser rechazada
        assertThatThrownBy(() -> useCase.getFoundationByRepresentativeId(3L))
                .isInstanceOf(ResourceNotFoundException.class);

        FoundationSummary withoutRep = new FoundationSummary(
                88L, null, "Suba", "Desc", "Dir", "Tel",
                "a@b.org", List.of("  SUPERIOR "), "Niñez", "08:00-17:00", "Política", true, null
        );
        FoundationSummary nullCats = new FoundationSummary(
                89L, "Zeta", "Suba", "Desc", "Dir", "Tel",
                "a@b.org", null, "Niñez", "08:00-17:00", "Política", true, null
        );
        when(foundationCatalogPort.findAllActiveFoundations()).thenReturn(List.of(withoutRep, nullCats));

        assertThatThrownBy(() -> useCase.getFoundationByRepresentativeId(88L))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(useCase.getActiveFoundations("   ")).hasSize(2);
        assertThat(useCase.getActiveFoundations("FOOTWEAR")).isEmpty();

        assertThatThrownBy(() -> useCase.getActiveFoundationById(null))
                .isInstanceOf(InvalidDonationRequestException.class);
        assertThatThrownBy(() -> useCase.getFoundationByRepresentativeId(null))
                .isInstanceOf(InvalidDonationRequestException.class);
        assertThatThrownBy(() -> useCase.getFoundationByRepresentativeId(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Debe emparejar grupos de ms-tiendas (TOP, BOTTOM, FOOTWEAR) pero respetar tipos específicos declarados por la fundación")
    void shouldMatchEnglishAcceptedCategoriesFromMsTiendas() {
        FoundationSummary qaFoundation = foundation(
                1L,
                "Fundación Pruebas QA",
                List.of("FOOTWEAR", "TOP", "BOTTOM"),
                true
        );
        FoundationSummary skirtsOnlyFoundation = foundation(
                2L,
                "Fundación Solo Faldas",
                List.of("FALDAS"),
                true
        );
        when(foundationCatalogPort.findAllActiveFoundations()).thenReturn(List.of(qaFoundation, skirtsOnlyFoundation));

        // 1. Fundación acepta BOTTOM y usuario busca Pantalones -> debe aparecer
        assertThat(useCase.getActiveFoundations("Pantalones")).extracting(FoundationSummary::id).containsExactly(1L);
        // 2. Fundación acepta únicamente FALDAS y usuario busca Pantalones -> solo aparece la de BOTTOM, FALDAS queda fuera
        assertThat(useCase.getActiveFoundations("Faldas")).extracting(FoundationSummary::id).containsExactly(1L, 2L);
        assertThat(useCase.getActiveFoundations("Chaquetas")).extracting(FoundationSummary::id).containsExactly(1L);
        assertThat(useCase.getActiveFoundations("TOP")).extracting(FoundationSummary::id).containsExactly(1L);
        assertThat(useCase.getActiveFoundations("CALZADO")).extracting(FoundationSummary::id).containsExactly(1L);
        assertThat(useCase.getActiveFoundations("ACCESORIOS")).isEmpty();

        // Verificación aislada de regresión para fundación específica FALDAS
        when(foundationCatalogPort.findAllActiveFoundations()).thenReturn(List.of(skirtsOnlyFoundation));
        assertThat(useCase.getActiveFoundations("Pantalones")).isEmpty();
        assertThat(useCase.getActiveFoundations("Faldas")).extracting(FoundationSummary::id).containsExactly(2L);
    }
}
