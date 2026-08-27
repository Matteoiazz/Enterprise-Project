package com.tripify.communication_service.service;

import com.tripify.communication_service.client.BookingClient;
import com.tripify.communication_service.entity.Review;
import com.tripify.communication_service.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingClient bookingClient;

    public Review createReview(Integer rating, String comment, String travelerId, Long catalogItemId) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Il rating deve essere compreso tra 1 e 5");
        }

        boolean hasBooked = bookingClient.hasUserBookedItem(catalogItemId);
        if (!hasBooked) {
            throw new IllegalStateException("Accesso negato: puoi recensire solo le esperienze che hai effettivamente prenotato e confermato.");
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

    public List<Review> getReviewsByTraveler(String travelerId) {
        return reviewRepository.findByTravelerId(travelerId);
    }
}