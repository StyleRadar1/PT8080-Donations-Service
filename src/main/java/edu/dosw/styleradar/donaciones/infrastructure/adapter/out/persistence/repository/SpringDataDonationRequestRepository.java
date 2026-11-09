package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.dosw.styleradar.donaciones.domain.model.DonationRequestStatus;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.entity.DonationRequestEntity;

/**
 * Repositorio Spring Data JPA para la entidad DonationRequestEntity.
 */
@Repository
public interface SpringDataDonationRequestRepository extends JpaRepository<DonationRequestEntity, Long> {

    List<DonationRequestEntity> findByDonorIdOrderByRequestedAtDesc(Long donorId);

    List<DonationRequestEntity> findByFoundationIdOrderByRequestedAtDesc(Long foundationId);

    List<DonationRequestEntity> findByFoundationIdAndStatusOrderByRequestedAtDesc(
            Long foundationId, DonationRequestStatus status);

    boolean existsByPublicationIdAndStatusIn(Long publicationId, Collection<DonationRequestStatus> statuses);
}
