package com.tripify.catalog_service.repository.spec;

import com.tripify.catalog_service.entity.CatalogItem;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CatalogItemSpecification {

    public static Specification<CatalogItem> withDynamicFilters(String category, String keyword, BigDecimal maxPrice, Integer minRating) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Filtro Categoria (Ignora se è "Tutti" o null)
            if (category != null && !category.equalsIgnoreCase("Tutti")) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("category")), category.toLowerCase()));
            }

            // 2. Filtro Keyword (Cerca in titolo OR descrizione)
            if (keyword != null && !keyword.trim().isEmpty()) {
                String likePattern = "%" + keyword.toLowerCase() + "%";
                Predicate titleMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), likePattern);
                Predicate descMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), likePattern);
                predicates.add(criteriaBuilder.or(titleMatch, descMatch));
            }

            // 3. Filtro Prezzo Massimo
            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            // 4. Filtro Rating Minimo
            if (minRating != null && minRating > 0) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("rating"), minRating));
            }

            // Unisce tutti i pezzi creati con un AND
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}