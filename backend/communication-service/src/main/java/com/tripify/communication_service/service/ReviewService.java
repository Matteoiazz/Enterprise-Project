package com.tripify.communication_service.service;

import com.tripify.communication_service.entity.Review;
import com.tripify.communication_service.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public Review createReview(Integer rating, String comment, Long travelerId, Long catalogItemId) {
        // Piccola validazione di business
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Il rating deve essere compreso tra 1 e 5 stelle");
        }
        if (comment == null || comment.trim().isEmpty()) {
            throw new IllegalArgumentException("Il testo della recensione è obbligatorio");
        }

        Review review = Review.builder()
                .rating(rating)
                .comment(comment)
                .travelerId(travelerId)
                .catalogItemId(catalogItemId)
                .build();

        return reviewRepository.save(review);
    }

    public List<Review> getReviewsByItem(Long catalogItemId) {
        return reviewRepository.findByCatalogItemId(catalogItemId);
    }

    public List<Review> getReviewsByTraveler(Long travelerId) {
        return reviewRepository.findByTravelerId(travelerId);
    }
}
