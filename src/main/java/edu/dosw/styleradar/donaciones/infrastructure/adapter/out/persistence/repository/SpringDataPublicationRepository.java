package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.entity.SecondHandPublicationEntity;

/**
 * Repositorio Spring Data JPA para la entidad SecondHandPublicationEntity.
 */
@Repository
public interface SpringDataPublicationRepository
        extends JpaRepository<SecondHandPublicationEntity, Long>,
                JpaSpecificationExecutor<SecondHandPublicationEntity> {

    List<SecondHandPublicationEntity> findByUserIdOrderByPublishedAtDesc(Long userId);
}
