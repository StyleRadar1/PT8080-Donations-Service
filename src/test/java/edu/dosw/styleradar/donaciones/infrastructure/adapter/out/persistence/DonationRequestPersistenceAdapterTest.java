package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequestStatus;
import edu.dosw.styleradar.donaciones.domain.model.GarmentType;
import edu.dosw.styleradar.donaciones.domain.model.PublicationStatus;
import edu.dosw.styleradar.donaciones.domain.model.Size;
import edu.dosw.styleradar.donaciones.domain.model.Style;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.entity.SecondHandPublicationEntity;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.mapper.DonationRequestPersistenceMapper;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.repository.SpringDataDonationRequestRepository;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.repository.SpringDataPublicationRepository;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({DonationRequestPersistenceAdapter.class, DonationRequestPersistenceMapper.class})
class DonationRequestPersistenceAdapterTest {

    @Autowired
    private DonationRequestPersistenceAdapter adapter;

    @Autowired
    private SpringDataDonationRequestRepository repository;

    @Autowired
    private SpringDataPublicationRepository publicationRepository;

    private Long testPublicationId1;
    private Long testPublicationId2;
    private Long testPublicationId3;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        publicationRepository.deleteAll();

        SecondHandPublicationEntity pub1 = publicationRepository.save(SecondHandPublicationEntity.builder()
                .userId(101L)
                .garmentName("Chaqueta")
                .garmentType(GarmentType.UPPER)
                .brand("Levi's")
                .color("Azul")
                .style(Style.URBAN)
                .size(Size.M)
                .conservationStatus(ConservationStatus.LIKE_NEW)
                .price(new BigDecimal("50000.00"))
                .status(PublicationStatus.IN_DONATION)
                .publishedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .photos(new ArrayList<>())
                .build());
        testPublicationId1 = pub1.getId();

        SecondHandPublicationEntity pub2 = publicationRepository.save(SecondHandPublicationEntity.builder()
                .userId(101L)
                .garmentName("Camisa")
                .garmentType(GarmentType.UPPER)
                .brand("Zara")
                .color("Blanco")
                .style(Style.CASUAL)
                .size(Size.L)
                .conservationStatus(ConservationStatus.GOOD_CONDITION)
                .price(new BigDecimal("40000.00"))
                .status(PublicationStatus.IN_DONATION)
                .publishedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .photos(new ArrayList<>())
                .build());
        testPublicationId2 = pub2.getId();

        SecondHandPublicationEntity pub3 = publicationRepository.save(SecondHandPublicationEntity.builder()
                .userId(102L)
                .garmentName("Pantalón")
                .garmentType(GarmentType.LOWER)
                .brand("Pull&Bear")
                .color("Negro")
                .style(Style.URBAN)
                .size(Size.M)
                .conservationStatus(ConservationStatus.GOOD_CONDITION)
                .price(new BigDecimal("60000.00"))
                .status(PublicationStatus.IN_DONATION)
                .publishedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .photos(new ArrayList<>())
                .build());
        testPublicationId3 = pub3.getId();
    }

    private DonationRequest buildSampleRequest(Long pubId, Long donorId, Long foundationId, DonationRequestStatus status) {
        return DonationRequest.builder()
                .publicationId(pubId)
                .donorId(donorId)
                .foundationId(foundationId)
                .message("Deseo donar esta prenda")
                .status(status)
                .requestedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Debe persistir y recuperar una solicitud de donación por ID")
    void shouldSaveAndFindById() {
        DonationRequest request = buildSampleRequest(testPublicationId1, 101L, 501L, DonationRequestStatus.PENDING);

        DonationRequest saved = adapter.save(request);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPublicationId()).isEqualTo(testPublicationId1);
        assertThat(saved.getDonorId()).isEqualTo(101L);

        Optional<DonationRequest> found = adapter.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(DonationRequestStatus.PENDING);
    }

    @Test
    @DisplayName("Debe listar solicitudes por donante")
    void shouldFindByDonorId() {
        adapter.save(buildSampleRequest(testPublicationId1, 101L, 501L, DonationRequestStatus.PENDING));
        adapter.save(buildSampleRequest(testPublicationId2, 101L, 502L, DonationRequestStatus.APPROVED));
        adapter.save(buildSampleRequest(testPublicationId3, 102L, 501L, DonationRequestStatus.PENDING));

        List<DonationRequest> list = adapter.findByDonorId(101L);

        assertThat(list).hasSize(2).allMatch(r -> r.getDonorId().equals(101L));
    }

    @Test
    @DisplayName("Debe listar solicitudes por fundación y por estado")
    void shouldFindByFoundationIdAndStatus() {
        adapter.save(buildSampleRequest(testPublicationId1, 101L, 501L, DonationRequestStatus.PENDING));
        adapter.save(buildSampleRequest(testPublicationId2, 102L, 501L, DonationRequestStatus.APPROVED));
        adapter.save(buildSampleRequest(testPublicationId3, 103L, 502L, DonationRequestStatus.PENDING));

        List<DonationRequest> byFoundation = adapter.findByFoundationId(501L);
        assertThat(byFoundation).hasSize(2);

        List<DonationRequest> byFoundationPending = adapter.findByFoundationIdAndStatus(501L, DonationRequestStatus.PENDING);
        assertThat(byFoundationPending).hasSize(1);
        assertThat(byFoundationPending.get(0).getStatus()).isEqualTo(DonationRequestStatus.PENDING);
    }

    @Test
    @DisplayName("Debe comprobar si existe una solicitud activa para una publicación")
    void shouldCheckActiveRequestForPublication() {
        adapter.save(buildSampleRequest(testPublicationId1, 101L, 501L, DonationRequestStatus.PENDING));
        adapter.save(buildSampleRequest(testPublicationId2, 101L, 501L, DonationRequestStatus.DELIVERED));

        assertThat(adapter.existsActiveRequestForPublication(testPublicationId1)).isTrue();
        assertThat(adapter.existsActiveRequestForPublication(testPublicationId2)).isFalse();
        assertThat(adapter.existsActiveRequestForPublication(999L)).isFalse();
    }
}
