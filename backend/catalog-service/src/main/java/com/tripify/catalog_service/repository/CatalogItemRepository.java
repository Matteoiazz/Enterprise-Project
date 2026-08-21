package com.tripify.catalog_service.repository;

import com.tripify.catalog_service.entity.CatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID; // Aggiunto per l'UUID

@Repository
public interface CatalogItemRepository extends JpaRepository<CatalogItem, Long>, JpaSpecificationExecutor<CatalogItem> {

    List<CatalogItem> findByHostId(UUID hostId);

    @Query(value = """
    SELECT DISTINCT city FROM (
        SELECT departure_city AS city FROM flight_details
        UNION
        SELECT arrival_city AS city FROM flight_details
        UNION
        SELECT city FROM hotel_details
        UNION
        SELECT city FROM activity_details
    ) AS all_cities
    WHERE LOWER(city) LIKE LOWER(CONCAT(:prefix, '%'))
    ORDER BY city
    LIMIT 10
    """, nativeQuery = true)
    List<String> findCitySuggestions(@Param("prefix") String prefix);

}