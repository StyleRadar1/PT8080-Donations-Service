package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import edu.dosw.styleradar.donaciones.domain.model.PublicationSearchCriteria;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.entity.SecondHandPublicationEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Especificaciones JPA dinámicas para la búsqueda y filtrado de publicaciones de economía circular.
 * Soporta filtros por texto, atributos de prenda, discriminador 'esDonacion', localidad de Bogotá y precios.
 */
public final class PublicationSpecifications {

    private PublicationSpecifications() {
    }

    public static Specification<SecondHandPublicationEntity> withCriteria(PublicationSearchCriteria criteria) {
        return (root, query, cb) -> {
            if (criteria == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();
            addSearchTermPredicate(criteria, root, cb, predicates);
            addGarmentAttributePredicates(criteria, root, cb, predicates);
            addPriceAndStylePredicates(criteria, root, cb, predicates);
            addCircularEconomyPredicates(criteria, root, cb, predicates);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void addSearchTermPredicate(PublicationSearchCriteria criteria,
                                                Root<SecondHandPublicationEntity> root,
                                                CriteriaBuilder cb,
                                                List<Predicate> predicates) {
        if (criteria.getSearchTerm() != null && !criteria.getSearchTerm().trim().isEmpty()) {
            String pattern = "%" + criteria.getSearchTerm().trim().toLowerCase() + "%";
            Predicate nameLike = cb.like(cb.lower(root.get("garmentName")), pattern);
            Predicate descLike = cb.like(cb.lower(root.get("description")), pattern);
            Predicate brandLike = cb.like(cb.lower(root.get("brand")), pattern);
            predicates.add(cb.or(nameLike, descLike, brandLike));
        }
    }

    private static void addGarmentAttributePredicates(PublicationSearchCriteria criteria,
                                                      Root<SecondHandPublicationEntity> root,
                                                      CriteriaBuilder cb,
                                                      List<Predicate> predicates) {
        if (criteria.getType() != null) {
            predicates.add(cb.equal(root.get("garmentType"), criteria.getType()));
        }
        if (criteria.getColor() != null && !criteria.getColor().trim().isEmpty()) {
            predicates.add(cb.equal(cb.lower(root.get("color")), criteria.getColor().trim().toLowerCase()));
        }
        if (criteria.getSize() != null) {
            predicates.add(cb.equal(root.get("size"), criteria.getSize()));
        }
        if (criteria.getConservationStatus() != null) {
            predicates.add(cb.equal(root.get("conservationStatus"), criteria.getConservationStatus()));
        }
        if (criteria.getBrand() != null && !criteria.getBrand().trim().isEmpty()) {
            predicates.add(cb.equal(cb.lower(root.get("brand")), criteria.getBrand().trim().toLowerCase()));
        }
    }

    private static void addPriceAndStylePredicates(PublicationSearchCriteria criteria,
                                                   Root<SecondHandPublicationEntity> root,
                                                   CriteriaBuilder cb,
                                                   List<Predicate> predicates) {
        if (criteria.getMinPrice() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("price"), criteria.getMinPrice()));
        }
        if (criteria.getMaxPrice() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("price"), criteria.getMaxPrice()));
        }
        if (criteria.getStyle() != null) {
            predicates.add(cb.equal(root.get("style"), criteria.getStyle()));
        }
        if (criteria.getStatus() != null) {
            predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
        }
    }

    private static void addCircularEconomyPredicates(PublicationSearchCriteria criteria,
                                                     Root<SecondHandPublicationEntity> root,
                                                     CriteriaBuilder cb,
                                                     List<Predicate> predicates) {
        if (criteria.getEsDonacion() != null) {
            predicates.add(cb.equal(root.get("esDonacion"), criteria.getEsDonacion()));
        }
        if (criteria.getLocalidad() != null) {
            predicates.add(cb.equal(root.get("localidad"), criteria.getLocalidad()));
        }
    }
}
