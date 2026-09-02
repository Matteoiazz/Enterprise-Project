package com.tripify.communication_service.repository;

import com.tripify.communication_service.entity.ReviewVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Repository
public interface ReviewVoteRepository extends JpaRepository<ReviewVote, Long> {

    boolean existsByReviewIdAndVoterId(Long reviewId, String voterId);

    long countByReviewId(Long reviewId);

    List<ReviewVote> findByReviewIdIn(Collection<Long> reviewIds);

    @Modifying
    @Transactional
    void deleteByReviewIdAndVoterId(Long reviewId, String voterId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ReviewVote v WHERE v.reviewId = :reviewId")
    void deleteByReviewId(@Param("reviewId") Long reviewId);
}
