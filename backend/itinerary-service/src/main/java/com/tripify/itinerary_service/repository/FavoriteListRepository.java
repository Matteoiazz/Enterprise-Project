package com.tripify.itinerary_service.repository;

import com.tripify.itinerary_service.entity.FavoriteList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FavoriteListRepository extends JpaRepository<FavoriteList, Long> {
    // Trova tutte le liste create da un utente specifico
    List<FavoriteList> findByOwnerId(Long ownerId);

    // Trova le liste condivise con un utente specifico
    List<FavoriteList> findBySharedUserIdsContaining(Long userId);
}
