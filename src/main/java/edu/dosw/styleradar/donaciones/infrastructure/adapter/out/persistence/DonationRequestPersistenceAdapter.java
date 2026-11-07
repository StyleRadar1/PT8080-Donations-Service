package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequestStatus;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationRequestRepositoryPort;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.entity.DonationRequestEntity;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.entity.SecondHandPublicationEntity;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.mapper.DonationRequestPersistenceMapper;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.repository.SpringDataDonationRequestRepository;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.repository.SpringDataPublicationRepository;
import lombok.RequiredArgsConstructor;

/**
 * Adaptador de salida (Driven Adapter) que implementa DonationRequestRepositoryPort mediante Spring Data JPA.
 */
@Component
@RequiredArgsConstructor
public class DonationRequestPersistenceAdapter implements DonationRequestRepositoryPort {

    private final SpringDataDonationRequestRepository donationRequestRepository;
    private final SpringDataPublicationRepository publicationRepository;
    private final DonationRequestPersistenceMapper donationRequestMapper;

    @Override
    @Transactional
    public DonationRequest save(DonationRequest request) {
        SecondHandPublicationEntity publication = publicationRepository.findById(request.getPublicationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Publicación no encontrada con ID: " + request.getPublicationId()));

        DonationRequestEntity entity = donationRequestMapper.toEntity(request, publication);
        DonationRequestEntity saved = donationRequestRepository.save(entity);
        return donationRequestMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DonationRequest> findById(Long id) {
        return donationRequestRepository.findById(id).map(donationRequestMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DonationRequest> findByDonorId(Long donorId) {
        return donationRequestRepository.findByDonorIdOrderByRequestedAtDesc(donorId)
                .stream()
                .map(donationRequestMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DonationRequest> findByFoundationId(Long foundationId) {
        return donationRequestRepository.findByFoundationIdOrderByRequestedAtDesc(foundationId)
                .stream()
                .map(donationRequestMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DonationRequest> findByFoundationIdAndStatus(Long foundationId, DonationRequestStatus status) {
        return donationRequestRepository.findByFoundationIdAndStatusOrderByRequestedAtDesc(foundationId, status)
                .stream()
                .map(donationRequestMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsActiveRequestForPublication(Long publicationId) {
        return donationRequestRepository.existsByPublicationIdAndStatusIn(
                publicationId, DonationRequestStatus.ACTIVE_STATUSES);
    }
}
