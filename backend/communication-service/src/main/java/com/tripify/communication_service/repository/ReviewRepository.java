package com.tripify.communication_service.repository;

import com.tripify.communication_service.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByCatalogItemId(Long catalogItemId);

    List<Review> findByTravelerId(String travelerId);

    boolean existsByTravelerIdAndCatalogItemId(String travelerId, Long catalogItemId);

    @Query("SELECT DISTINCT r.catalogItemId FROM Review r")
    List<Long> findDistinctCatalogItemIds();
}
