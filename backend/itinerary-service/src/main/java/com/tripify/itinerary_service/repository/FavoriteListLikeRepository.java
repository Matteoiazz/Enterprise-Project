package com.tripify.itinerary_service.repository;

import com.tripify.itinerary_service.entity.FavoriteListLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteListLikeRepository extends JpaRepository<FavoriteListLike, Long> {
    Optional<FavoriteListLike> findByListIdAndUserId(Long listId, String userId);

    boolean existsByListIdAndUserId(Long listId, String userId);

    List<FavoriteListLike> findByUserId(String userId);

    void deleteByListIdAndUserId(Long listId, String userId);

    void deleteByListId(Long listId);
}
