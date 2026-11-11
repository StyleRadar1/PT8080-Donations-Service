package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import edu.dosw.styleradar.donaciones.domain.model.ConservationStatus;
import edu.dosw.styleradar.donaciones.domain.model.Garment;
import edu.dosw.styleradar.donaciones.domain.model.GarmentType;
import edu.dosw.styleradar.donaciones.domain.model.PublicationSearchCriteria;
import edu.dosw.styleradar.donaciones.domain.model.PublicationStatus;
import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;
import edu.dosw.styleradar.donaciones.domain.model.Size;
import edu.dosw.styleradar.donaciones.domain.model.Style;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.mapper.PublicationPersistenceMapper;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.repository.SpringDataPublicationRepository;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PublicationPersistenceAdapter.class, PublicationPersistenceMapper.class})
class PublicationPersistenceAdapterTest {

    @Autowired
    private PublicationPersistenceAdapter adapter;

    @Autowired
    private SpringDataPublicationRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    private SecondHandPublication buildSamplePublication(Long userId, String name, BigDecimal price) {
        Garment garment = Garment.builder()
                .name(name)
                .description("Descripción de prueba")
                .type(GarmentType.UPPER)
                .brand("Levi's")
                .color("Azul")
                .style(Style.URBAN)
                .build();

        return SecondHandPublication.builder()
                .userId(userId)
                .garment(garment)
                .size(Size.M)
                .conservationStatus(ConservationStatus.LIKE_NEW)
                .price(price)
                .status(PublicationStatus.AVAILABLE)
                .publishedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .photos(List.of("https://cdn.test.com/foto1.jpg", "https://cdn.test.com/foto2.jpg"))
                .build();
    }

    @Test
    @DisplayName("Debe persistir y recuperar una publicación de segunda mano con sus fotos")
    void shouldSaveAndFindById() {
        SecondHandPublication publication = buildSamplePublication(101L, "Chaqueta Vintage", new BigDecimal("50000.00"));

        SecondHandPublication saved = adapter.save(publication);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(101L);
        assertThat(saved.getGarment().getName()).isEqualTo("Chaqueta Vintage");
        assertThat(saved.getPhotos()).hasSize(2);

        Optional<SecondHandPublication> found = adapter.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getGarment().getBrand()).isEqualTo("Levi's");
    }

    @Test
    @DisplayName("Debe listar las publicaciones asociadas a un usuario específico")
    void shouldFindByUserId() {
        adapter.save(buildSamplePublication(201L, "Prenda 1", new BigDecimal("30000.00")));
        adapter.save(buildSamplePublication(201L, "Prenda 2", new BigDecimal("40000.00")));
        adapter.save(buildSamplePublication(301L, "Prenda Otro Usuario", new BigDecimal("50000.00")));

        List<SecondHandPublication> userPublications = adapter.findByUserId(201L);

        assertThat(userPublications).hasSize(2).allMatch(p -> p.getUserId().equals(201L));
    }

    @Test
    @DisplayName("Debe filtrar publicaciones por criterios de búsqueda combinados")
    void shouldFindByCriteria() {
        adapter.save(buildSamplePublication(101L, "Chaqueta Denim", new BigDecimal("50000.00")));
        adapter.save(buildSamplePublication(102L, "Camisa Formal", new BigDecimal("80000.00")));

        PublicationSearchCriteria criteria = PublicationSearchCriteria.builder()
                .searchTerm("Denim")
                .type(GarmentType.UPPER)
                .brand("Levi's")
                .color("Azul")
                .size(Size.M)
                .conservationStatus(ConservationStatus.LIKE_NEW)
                .style(Style.URBAN)
                .minPrice(new BigDecimal("40000.00"))
                .maxPrice(new BigDecimal("60000.00"))
                .status(PublicationStatus.AVAILABLE)
                .build();

        List<SecondHandPublication> results = adapter.findByCriteria(criteria);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getGarment().getName()).isEqualTo("Chaqueta Denim");

        List<SecondHandPublication> allResults = adapter.findByCriteria(null);
        assertThat(allResults).hasSize(2);
    }

    @Test
    @DisplayName("Debe verificar existencia por ID y eliminar correctamente")
    void shouldCheckExistsAndDelete() {
        SecondHandPublication saved = adapter.save(buildSamplePublication(101L, "Pantalón", new BigDecimal("45000.00")));

        assertThat(adapter.existsById(saved.getId())).isTrue();

        adapter.delete(saved.getId());

        assertThat(adapter.existsById(saved.getId())).isFalse();
        assertThat(adapter.findById(saved.getId())).isEmpty();
    }
}
