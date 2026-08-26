package com.tripify.itinerary_service.repository;

import com.tripify.itinerary_service.entity.CatalogItemLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CatalogItemLikeRepository extends JpaRepository<CatalogItemLike, Long> {
    Optional<CatalogItemLike> findByUserIdAndCatalogItemId(String userId, Long catalogItemId);

    boolean existsByUserIdAndCatalogItemId(String userId, Long catalogItemId);

    List<CatalogItemLike> findByUserIdOrderByCreatedAtDesc(String userId);
}
