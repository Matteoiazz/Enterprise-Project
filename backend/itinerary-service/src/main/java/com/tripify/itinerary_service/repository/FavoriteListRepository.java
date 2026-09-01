package com.tripify.itinerary_service.repository;

import com.tripify.itinerary_service.entity.FavoriteList;
import com.tripify.itinerary_service.entity.Visibility;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteListRepository extends JpaRepository<FavoriteList, Long> {
    // Trova tutte le liste create da un utente specifico
    List<FavoriteList> findByOwnerId(String ownerId);

    // Trova le liste condivise con un utente specifico
    List<FavoriteList> findBySharedUserIdsContaining(String userId);

    Optional<FavoriteList> findByPublicToken(String publicToken);

    Optional<FavoriteList> findByCollabToken(String collabToken);

    // Feed pubblico: solo liste PUBLIC, opzionalmente filtrate per città. Il Pageable
    // limita quante righe arrivano dal DB (vedi ItineraryService.getPublicFeed): un
    // endpoint anonimo non deve poter far restituire l'intera tabella in una chiamata.
    List<FavoriteList> findByVisibilityOrderByLikesCountDesc(Visibility visibility, Pageable pageable);

    List<FavoriteList> findByVisibilityOrderByCreatedAtDesc(Visibility visibility, Pageable pageable);

    List<FavoriteList> findByVisibilityAndCityIgnoreCaseOrderByLikesCountDesc(Visibility visibility, String city, Pageable pageable);

    List<FavoriteList> findByVisibilityAndCityIgnoreCaseOrderByCreatedAtDesc(Visibility visibility, String city, Pageable pageable);
}
