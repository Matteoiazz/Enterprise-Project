package com.tripify.communication_service.service;

import com.tripify.communication_service.client.BookingClient;
import com.tripify.communication_service.client.CatalogClient;
import com.tripify.communication_service.dto.ReviewResponse;
import com.tripify.communication_service.entity.Review;
import com.tripify.communication_service.entity.ReviewVote;
import com.tripify.communication_service.repository.ReviewRepository;
import com.tripify.communication_service.repository.ReviewVoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewVoteRepository reviewVoteRepository;
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

        Review saved;
        try {
            saved = reviewRepository.save(review);
        } catch (DataIntegrityViolationException duplicate) {
            throw new IllegalStateException("Hai già recensito questa esperienza");
        }
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
        reviewVoteRepository.deleteByReviewId(review.getId());
        reviewRepository.delete(review);
        recomputeItemRating(catalogItemId);
    }

    public List<ReviewResponse> getReviewsByItem(Long catalogItemId, String callerId) {
        List<Review> reviews = reviewRepository.findByCatalogItemId(catalogItemId);
        return withHelpfulData(reviews, callerId, review -> {
            boolean ownedByCaller = callerId != null && callerId.equals(review.getTravelerId());
            return ownedByCaller ? review.getTravelerId() : null;
        });
    }

    public List<ReviewResponse> getReviewsByTraveler(String travelerId) {
        List<Review> reviews = reviewRepository.findByTravelerId(travelerId);
        return withHelpfulData(reviews, travelerId, Review::getTravelerId);
    }

    private List<ReviewResponse> withHelpfulData(List<Review> reviews, String callerId,
                                                 java.util.function.Function<Review, String> travelerIdResolver) {
        if (reviews.isEmpty()) {
            return List.of();
        }
        List<Long> reviewIds = reviews.stream().map(Review::getId).toList();
        List<ReviewVote> votes = reviewVoteRepository.findByReviewIdIn(reviewIds);

        Map<Long, Long> countByReview = votes.stream()
                .collect(Collectors.groupingBy(ReviewVote::getReviewId, Collectors.counting()));
        Set<Long> votedByCaller = callerId == null ? Set.of() : votes.stream()
                .filter(v -> callerId.equals(v.getVoterId()))
                .map(ReviewVote::getReviewId)
                .collect(Collectors.toSet());

        return reviews.stream()
                .map(review -> new ReviewResponse(
                        review.getId(),
                        review.getRating(),
                        review.getComment(),
                        travelerIdResolver.apply(review),
                        review.getCatalogItemId(),
                        review.getReply(),
                        review.getRepliedAt(),
                        countByReview.getOrDefault(review.getId(), 0L).intValue(),
                        votedByCaller.contains(review.getId())))
                .toList();
    }

    public ReviewResponse toggleHelpful(Long reviewId, String voterId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NoSuchElementException("Recensione non trovata"));

        if (review.getTravelerId().equals(voterId)) {
            throw new IllegalStateException("Non puoi votare la tua stessa recensione");
        }

        boolean votedNow;
        if (reviewVoteRepository.existsByReviewIdAndVoterId(reviewId, voterId)) {
            reviewVoteRepository.deleteByReviewIdAndVoterId(reviewId, voterId);
            votedNow = false;
        } else {
            try {
                reviewVoteRepository.save(ReviewVote.builder().reviewId(reviewId).voterId(voterId).build());
                votedNow = true;
            } catch (DataIntegrityViolationException alreadyVoted) {
                reviewVoteRepository.deleteByReviewIdAndVoterId(reviewId, voterId);
                votedNow = false;
            }
        }

        int helpfulCount = (int) reviewVoteRepository.countByReviewId(reviewId);
        return new ReviewResponse(
                review.getId(),
                review.getRating(),
                review.getComment(),
                null,
                review.getCatalogItemId(),
                review.getReply(),
                review.getRepliedAt(),
                helpfulCount,
                votedNow
        );
    }

    public ReviewResponse replyToReview(Long id, String reply, String requesterId) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Recensione non trovata"));

        String hostId = catalogClient.getItem(review.getCatalogItemId()).hostId();
        if (hostId == null || !hostId.equals(requesterId)) {
            throw new IllegalStateException("Solo l'organizzatore dell'annuncio può rispondere a questa recensione");
        }

        review.setReply(reply.trim());
        review.setRepliedAt(java.time.Instant.now());
        return ReviewResponse.from(reviewRepository.save(review));
    }

    public void reconcileAllRatings() {
        reviewRepository.findDistinctCatalogItemIds().forEach(this::recomputeItemRating);
    }

    private void recomputeItemRating(Long catalogItemId) {
        try {
            List<Review> all = reviewRepository.findByCatalogItemId(catalogItemId);
            if (all.isEmpty()) {
                catalogClient.updateRating(catalogItemId, internalServiceKey, new CatalogClient.RatingUpdate(null, 0));
            } else {
                double average = all.stream().mapToInt(Review::getRating).average().orElse(0.0);
                double rounded = Math.round(average * 10.0) / 10.0;
                catalogClient.updateRating(catalogItemId, internalServiceKey, new CatalogClient.RatingUpdate(rounded, all.size()));
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
