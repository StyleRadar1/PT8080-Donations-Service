package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;

import edu.dosw.styleradar.donaciones.domain.model.ConservationStatus;
import edu.dosw.styleradar.donaciones.domain.model.Garment;
import edu.dosw.styleradar.donaciones.domain.model.GarmentType;
import edu.dosw.styleradar.donaciones.domain.model.PublicationSearchCriteria;
import edu.dosw.styleradar.donaciones.domain.model.PublicationStatus;
import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;
import edu.dosw.styleradar.donaciones.domain.model.Size;
import edu.dosw.styleradar.donaciones.domain.model.Style;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.converter.GarmentTypeConverter;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.converter.SizeConverter;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.mapper.PublicationPersistenceMapper;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.repository.SpringDataPublicationRepository;
import jakarta.persistence.EntityManager;

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

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

    @Test
    @DisplayName("Debe cubrir filtros vacíos, esDonacion y localidad en PublicationSpecifications")
    void shouldFilterByCircularEconomyBlankStringsAndNullCriteriaFields() {
        SecondHandPublication donation = buildSamplePublication(101L, "Abrigo Chapinero", BigDecimal.ZERO);
        donation.setEsDonacion(true);
        donation.setLocalidad(edu.dosw.styleradar.donaciones.domain.model.LocalidadBogota.USAQUEN);
        adapter.save(donation);
        adapter.save(buildSamplePublication(102L, "Jean Comercial", new BigDecimal("65000.00")));

        PublicationSearchCriteria circularCriteria = PublicationSearchCriteria.builder()
                .searchTerm("   ")
                .color(" ")
                .brand("")
                .esDonacion(true)
                .localidad(edu.dosw.styleradar.donaciones.domain.model.LocalidadBogota.USAQUEN)
                .build();

        List<SecondHandPublication> circularResults = adapter.findByCriteria(circularCriteria);
        assertThat(circularResults).hasSize(1);
        assertThat(circularResults.get(0).isEsDonacion()).isTrue();

        PublicationSearchCriteria emptyCriteria = PublicationSearchCriteria.builder().build();
        assertThat(adapter.findByCriteria(emptyCriteria)).hasSize(2);
    }

    @Test
    @DisplayName("Debe leer registros históricos (UPPER, LOWER, DRESS, FOOTWEAR, ACCESSORY, ONE_SIZE, 28) y migrarlos con Flyway V5")
    void shouldReadLegacyDatabaseEnumValuesAndApplyV5Migration() throws Exception {
        insertLegacyRow(101L, "Camisa Legacy", "UPPER", "ONE_SIZE");
        insertLegacyRow(102L, "Pantalon Legacy", "LOWER", "28");
        insertLegacyRow(103L, "Vestido Legacy", "DRESS", "M");
        insertLegacyRow(104L, "Tenis Legacy", "FOOTWEAR", "38");
        insertLegacyRow(105L, "Bolso Legacy", "ACCESSORY", "Unica");

        entityManager.flush();
        entityManager.clear();

        List<Long> ids = jdbcTemplate.queryForList("SELECT id FROM publicaciones_segunda_mano ORDER BY id ASC", Long.class);
        assertThat(ids).hasSize(5);

        // Lectura tolerante antes de ejecutar V5
        SecondHandPublication upperLegacy = adapter.findById(ids.get(0)).orElseThrow();
        SecondHandPublication lowerLegacy = adapter.findById(ids.get(1)).orElseThrow();
        SecondHandPublication dressLegacy = adapter.findById(ids.get(2)).orElseThrow();
        SecondHandPublication footwearLegacy = adapter.findById(ids.get(3)).orElseThrow();
        SecondHandPublication accessoryLegacy = adapter.findById(ids.get(4)).orElseThrow();

        assertThat(upperLegacy.getGarment().getType()).isEqualTo(GarmentType.CAMISAS);
        assertThat(upperLegacy.getSize()).isEqualTo(Size.UNICA);
        assertThat(lowerLegacy.getGarment().getType()).isEqualTo(GarmentType.PANTALONES);
        assertThat(lowerLegacy.getSize()).isEqualTo(Size.TALLA_28);
        assertThat(dressLegacy.getGarment().getType()).isEqualTo(GarmentType.VESTIDOS);
        assertThat(footwearLegacy.getGarment().getType()).isEqualTo(GarmentType.CALZADO);
        assertThat(footwearLegacy.getSize()).isEqualTo(Size.TALLA_38);
        assertThat(accessoryLegacy.getGarment().getType()).isEqualTo(GarmentType.ACCESORIOS);
        assertThat(accessoryLegacy.getSize()).isEqualTo(Size.UNICA);

        // Ejecutar migración Flyway V5 dentro de la misma transacción JDBC de prueba
        entityManager.unwrap(Session.class).doWork(conn ->
                ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/migration/V5__migrate_garment_type_and_size_enums.sql")));

        entityManager.clear();

        List<Map<String, Object>> migratedRows = jdbcTemplate.queryForList(
                "SELECT tipo_prenda, talla FROM publicaciones_segunda_mano ORDER BY id ASC");
        assertThat(migratedRows.get(0)).containsEntry("TIPO_PRENDA", "CAMISAS").containsEntry("TALLA", "UNICA");
        assertThat(migratedRows.get(1)).containsEntry("TIPO_PRENDA", "PANTALONES").containsEntry("TALLA", "TALLA_28");
        assertThat(migratedRows.get(2)).containsEntry("TIPO_PRENDA", "VESTIDOS").containsEntry("TALLA", "M");
        assertThat(migratedRows.get(3)).containsEntry("TIPO_PRENDA", "CALZADO").containsEntry("TALLA", "TALLA_38");
        assertThat(migratedRows.get(4)).containsEntry("TIPO_PRENDA", "ACCESORIOS").containsEntry("TALLA", "UNICA");

        // Consultas JPQL / Specification tras la migración V5
        List<SecondHandPublication> migratedPantalones = adapter.findByCriteria(
                PublicationSearchCriteria.builder()
                        .type(GarmentType.PANTALONES)
                        .size(Size.TALLA_28)
                        .build());
        assertThat(migratedPantalones).hasSize(1);
        assertThat(migratedPantalones.get(0).getGarment().getName()).isEqualTo("Pantalon Legacy");

        GarmentTypeConverter garmentTypeConverter = new GarmentTypeConverter();
        SizeConverter sizeConverter = new SizeConverter();
        assertThat(garmentTypeConverter.convertToDatabaseColumn(null)).isNull();
        assertThat(sizeConverter.convertToDatabaseColumn(null)).isNull();
    }

    private void insertLegacyRow(Long userId, String garmentName, String legacyType, String legacySize) {
        jdbcTemplate.update("""
                INSERT INTO publicaciones_segunda_mano
                (usuario_id, nombre_prenda, descripcion, tipo_prenda, marca, color, estilo, talla, estado_conservacion, precio, es_donacion, localidad, estado, version, fecha_publicacion, fecha_actualizacion)
                VALUES (?, ?, 'Registro historico', ?, 'Marca Legacy', 'Negro', 'CASUAL', ?, 'GOOD_CONDITION', 45000.00, false, 'CHAPINERO', 'AVAILABLE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, userId, garmentName, legacyType, legacySize);
    }
}
