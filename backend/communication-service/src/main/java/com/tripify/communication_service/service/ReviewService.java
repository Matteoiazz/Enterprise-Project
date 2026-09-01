package com.tripify.communication_service.service;

import com.tripify.communication_service.client.BookingClient;
import com.tripify.communication_service.client.CatalogClient;
import com.tripify.communication_service.dto.ReviewResponse;
import com.tripify.communication_service.entity.Review;
import com.tripify.communication_service.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingClient bookingClient;
    private final CatalogClient catalogClient;

    @Value("${internal.service-key}")
    private String internalServiceKey;

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

        Review saved = reviewRepository.save(review);
        recomputeItemRating(catalogItemId);
        return ReviewResponse.from(saved);
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
        Review saved = reviewRepository.save(review);
        recomputeItemRating(review.getCatalogItemId());
        return ReviewResponse.from(saved);
    }

    public void deleteReview(Long id, String travelerId) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Recensione non trovata"));

        if (!review.getTravelerId().equals(travelerId)) {
            throw new IllegalStateException("Non puoi cancellare la recensione di un altro utente");
        }
        Long catalogItemId = review.getCatalogItemId();
        reviewRepository.delete(review);
        recomputeItemRating(catalogItemId);
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

    private void recomputeItemRating(Long catalogItemId) {
        try {
            List<Review> all = reviewRepository.findByCatalogItemId(catalogItemId);
            if (all.isEmpty()) {
                catalogClient.updateRating(catalogItemId, internalServiceKey, new CatalogClient.RatingUpdate(null, 0));
            } else {
                double average = all.stream().mapToInt(Review::getRating).average().orElse(0.0);
                catalogClient.updateRating(catalogItemId, internalServiceKey, new CatalogClient.RatingUpdate(average, all.size()));
            }
        } catch (Exception e) {
            log.warn("Rating dell'annuncio {} non aggiornato: {}", catalogItemId, e.getMessage());
        }
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
