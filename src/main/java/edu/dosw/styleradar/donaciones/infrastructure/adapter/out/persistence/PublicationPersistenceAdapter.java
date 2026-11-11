package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.donaciones.domain.model.PublicationSearchCriteria;
import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;
import edu.dosw.styleradar.donaciones.domain.port.out.PublicationRepositoryPort;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.entity.SecondHandPublicationEntity;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.mapper.PublicationPersistenceMapper;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.repository.PublicationSpecifications;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.repository.SpringDataPublicationRepository;
import lombok.RequiredArgsConstructor;

/**
 * Adaptador de salida (Driven Adapter) que implementa PublicationRepositoryPort mediante Spring Data JPA.
 */
@Component
@RequiredArgsConstructor
public class PublicationPersistenceAdapter implements PublicationRepositoryPort {

    private final SpringDataPublicationRepository publicationRepository;
    private final PublicationPersistenceMapper publicationMapper;

    @Override
    @Transactional
    public SecondHandPublication save(SecondHandPublication publication) {
        SecondHandPublicationEntity entity = publicationMapper.toEntity(publication);
        SecondHandPublicationEntity saved = publicationRepository.save(entity);
        return publicationMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SecondHandPublication> findById(Long id) {
        return publicationRepository.findById(id).map(publicationMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SecondHandPublication> findByUserId(Long userId) {
        return publicationRepository.findByUserIdOrderByPublishedAtDesc(userId)
                .stream()
                .map(publicationMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SecondHandPublication> findByCriteria(PublicationSearchCriteria criteria) {
        Specification<SecondHandPublicationEntity> spec = PublicationSpecifications.withCriteria(criteria);
        return publicationRepository.findAll(spec)
                .stream()
                .map(publicationMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return publicationRepository.existsById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        publicationRepository.deleteById(id);
    }
}
