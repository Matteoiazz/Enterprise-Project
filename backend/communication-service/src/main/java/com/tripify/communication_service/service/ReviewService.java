package com.tripify.communication_service.service;

import com.tripify.communication_service.client.BookingClient;
import com.tripify.communication_service.dto.ReviewResponse;
import com.tripify.communication_service.entity.Review;
import com.tripify.communication_service.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingClient bookingClient;

    public ReviewResponse createReview(Integer rating, String comment, String travelerId, Long catalogItemId) {
        validate(rating, comment);

        boolean hasBooked = bookingClient.hasUserBookedItem(catalogItemId);
        if (!hasBooked) {
            throw new IllegalStateException("Accesso negato: puoi recensire solo le esperienze che hai effettivamente prenotato e confermato.");
        }

        if (reviewRepository.existsByTravelerIdAndCatalogItemId(travelerId, catalogItemId)) {
            throw new IllegalStateException("Hai già recensito questa esperienza");
        }

        Review review = Review.builder()
                .rating(rating)
                .comment(comment)
                .travelerId(travelerId)
                .catalogItemId(catalogItemId)
                .build();

        return ReviewResponse.from(reviewRepository.save(review));
    }

    public ReviewResponse updateReview(Long id, Integer rating, String comment, String travelerId) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Recensione non trovata"));

        if (!review.getTravelerId().equals(travelerId)) {
            throw new IllegalStateException("Non puoi modificare la recensione di un altro utente");
        }
        validate(rating, comment);

        review.setRating(rating);
        review.setComment(comment);
        return ReviewResponse.from(reviewRepository.save(review));
    }

    public void deleteReview(Long id, String travelerId) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Recensione non trovata"));

        if (!review.getTravelerId().equals(travelerId)) {
            throw new IllegalStateException("Non puoi cancellare la recensione di un altro utente");
        }
        reviewRepository.delete(review);
    }

    public List<ReviewResponse> getReviewsByItem(Long catalogItemId) {
        return reviewRepository.findByCatalogItemId(catalogItemId).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    public List<ReviewResponse> getReviewsByTraveler(String travelerId) {
        return reviewRepository.findByTravelerId(travelerId).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    private void validate(Integer rating, String comment) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Il rating deve essere compreso tra 1 e 5");
        }
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("Il commento non può essere vuoto");
        }
        if (comment.length() > 1000) {
            throw new IllegalArgumentException("Il commento non può superare i 1000 caratteri");
        }
    }
}
