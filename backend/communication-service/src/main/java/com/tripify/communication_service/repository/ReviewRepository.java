package com.tripify.communication_service.repository;

import com.tripify.communication_service.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Trova tutte le recensioni per uno specifico Volo/Hotel/Attività
    List<Review> findByCatalogItemId(Long catalogItemId);

    // Trova tutte le recensioni scritte da un utente specifico
    List<Review> findByTravelerId(Long travelerId);
}