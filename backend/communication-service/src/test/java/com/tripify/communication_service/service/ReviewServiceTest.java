package com.tripify.communication_service.service;

import com.tripify.communication_service.client.BookingClient;
import com.tripify.communication_service.client.CatalogClient;
import com.tripify.communication_service.dto.ReviewResponse;
import com.tripify.communication_service.entity.Review;
import com.tripify.communication_service.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock ReviewRepository reviewRepository;
    @Mock BookingClient bookingClient;
    @Mock CatalogClient catalogClient;

    @InjectMocks ReviewService service;

    private static final String TRAVELER = "traveler-sub-1";
    private static final String HOST = "host-sub-1";
    private static final long ITEM = 42L;

    @BeforeEach
    void key() {
        ReflectionTestUtils.setField(service, "internalServiceKey", "test-key");
    }

    private Review review(long id, int rating, String traveler) {
        return Review.builder().id(id).rating(rating).comment("ok").travelerId(traveler).catalogItemId(ITEM).build();
    }

    @Test
    void createReview_rejectsRatingOutOfRange() {
        assertThatThrownBy(() -> service.createReview(0, "ottimo", TRAVELER, ITEM))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("1 e 5");
        assertThatThrownBy(() -> service.createReview(6, "ottimo", TRAVELER, ITEM))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createReview(null, "ottimo", TRAVELER, ITEM))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createReview_rejectsBlankComment() {
        assertThatThrownBy(() -> service.createReview(5, "   ", TRAVELER, ITEM))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("vuoto");
    }

    @Test
    void createReview_rejectsCommentOverThousandChars() {
        assertThatThrownBy(() -> service.createReview(5, "x".repeat(1001), TRAVELER, ITEM))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("1000");
    }

    @Test
    void createReview_whenUserHasNotBooked_throwsIllegalState() {
        when(bookingClient.hasUserBookedItem(ITEM)).thenReturn(false);

        assertThatThrownBy(() -> service.createReview(5, "ottimo", TRAVELER, ITEM))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("prenotato");
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_whenAlreadyReviewed_throwsIllegalState() {
        when(bookingClient.hasUserBookedItem(ITEM)).thenReturn(true);
        when(reviewRepository.existsByTravelerIdAndCatalogItemId(TRAVELER, ITEM)).thenReturn(true);

        assertThatThrownBy(() -> service.createReview(5, "ottimo", TRAVELER, ITEM))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("già recensito");
    }

    @Test
    void createReview_whenUniqueConstraintRace_mapsToIllegalState() {
        when(bookingClient.hasUserBookedItem(ITEM)).thenReturn(true);
        when(reviewRepository.existsByTravelerIdAndCatalogItemId(TRAVELER, ITEM)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenThrow(new DataIntegrityViolationException("uk_reviews"));

        assertThatThrownBy(() -> service.createReview(5, "ottimo", TRAVELER, ITEM))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("già recensito");
    }

    @Test
    void createReview_happyPath_savesAndRecomputesAverage() {
        Review saved = review(1L, 5, TRAVELER);
        when(bookingClient.hasUserBookedItem(ITEM)).thenReturn(true);
        when(reviewRepository.existsByTravelerIdAndCatalogItemId(TRAVELER, ITEM)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(saved);
        when(reviewRepository.findByCatalogItemId(ITEM)).thenReturn(List.of(review(1L, 5, TRAVELER), review(2L, 3, "t2")));

        ReviewResponse response = service.createReview(5, "ottimo", TRAVELER, ITEM);

        assertThat(response.rating()).isEqualTo(5);
        ArgumentCaptor<CatalogClient.RatingUpdate> captor = ArgumentCaptor.forClass(CatalogClient.RatingUpdate.class);
        verify(catalogClient).updateRating(eq(ITEM), eq("test-key"), captor.capture());
        assertThat(captor.getValue().average()).isEqualTo(4.0);
        assertThat(captor.getValue().count()).isEqualTo(2);
    }

    @Test
    void createReview_stillSucceedsIfRatingSyncFails() {
        when(bookingClient.hasUserBookedItem(ITEM)).thenReturn(true);
        when(reviewRepository.existsByTravelerIdAndCatalogItemId(TRAVELER, ITEM)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(review(1L, 5, TRAVELER));
        when(reviewRepository.findByCatalogItemId(ITEM)).thenReturn(List.of(review(1L, 5, TRAVELER)));
        org.mockito.Mockito.doThrow(new RuntimeException("catalog down"))
                .when(catalogClient).updateRating(anyLong(), any(), any());

        ReviewResponse response = service.createReview(5, "ottimo", TRAVELER, ITEM);

        assertThat(response).isNotNull();
    }

    @Test
    void updateReview_whenNotFound_throwsNoSuchElement() {
        when(reviewRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateReview(9L, 4, "modificato", TRAVELER))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void updateReview_whenNotOwner_throwsIllegalState() {
        when(reviewRepository.findById(9L)).thenReturn(Optional.of(review(9L, 5, "someone-else")));

        assertThatThrownBy(() -> service.updateReview(9L, 4, "modificato", TRAVELER))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("altro utente");
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void updateReview_happyPath_updatesFieldsAndRecomputes() {
        Review existing = review(9L, 5, TRAVELER);
        when(reviewRepository.findById(9L)).thenReturn(Optional.of(existing));
        when(reviewRepository.save(existing)).thenReturn(existing);
        when(reviewRepository.findByCatalogItemId(ITEM)).thenReturn(List.of(existing));

        service.updateReview(9L, 2, "peggiorato", TRAVELER);

        assertThat(existing.getRating()).isEqualTo(2);
        assertThat(existing.getComment()).isEqualTo("peggiorato");
        verify(catalogClient).updateRating(eq(ITEM), eq("test-key"), any());
    }

    @Test
    void deleteReview_whenNotOwner_throwsIllegalState() {
        when(reviewRepository.findById(9L)).thenReturn(Optional.of(review(9L, 5, "someone-else")));

        assertThatThrownBy(() -> service.deleteReview(9L, TRAVELER))
                .isInstanceOf(IllegalStateException.class);
        verify(reviewRepository, never()).delete(any());
    }

    @Test
    void deleteReview_whenLastReviewGone_resetsRatingToNull() {
        Review existing = review(9L, 5, TRAVELER);
        when(reviewRepository.findById(9L)).thenReturn(Optional.of(existing));
        when(reviewRepository.findByCatalogItemId(ITEM)).thenReturn(List.of());

        service.deleteReview(9L, TRAVELER);

        verify(reviewRepository).delete(existing);
        ArgumentCaptor<CatalogClient.RatingUpdate> captor = ArgumentCaptor.forClass(CatalogClient.RatingUpdate.class);
        verify(catalogClient).updateRating(eq(ITEM), eq("test-key"), captor.capture());
        assertThat(captor.getValue().average()).isNull();
        assertThat(captor.getValue().count()).isEqualTo(0);
    }

    @Test
    void replyToReview_whenNotFound_throwsNoSuchElement() {
        when(reviewRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.replyToReview(9L, "grazie", HOST))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void replyToReview_whenRequesterIsNotItemHost_throwsIllegalState() {
        when(reviewRepository.findById(9L)).thenReturn(Optional.of(review(9L, 5, TRAVELER)));
        when(catalogClient.getItem(ITEM)).thenReturn(new CatalogClient.CatalogItemView(ITEM, HOST));

        assertThatThrownBy(() -> service.replyToReview(9L, "grazie", "un-altro-utente"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("organizzatore");
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void replyToReview_whenHostIsNull_throwsIllegalState() {
        when(reviewRepository.findById(9L)).thenReturn(Optional.of(review(9L, 5, TRAVELER)));
        when(catalogClient.getItem(ITEM)).thenReturn(new CatalogClient.CatalogItemView(ITEM, null));

        assertThatThrownBy(() -> service.replyToReview(9L, "grazie", HOST))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void replyToReview_whenHost_savesTrimmedReplyWithTimestamp() {
        Review existing = review(9L, 5, TRAVELER);
        when(reviewRepository.findById(9L)).thenReturn(Optional.of(existing));
        when(catalogClient.getItem(ITEM)).thenReturn(new CatalogClient.CatalogItemView(ITEM, HOST));
        when(reviewRepository.save(existing)).thenReturn(existing);

        ReviewResponse response = service.replyToReview(9L, "  grazie per la recensione  ", HOST);

        assertThat(existing.getReply()).isEqualTo("grazie per la recensione");
        assertThat(existing.getRepliedAt()).isNotNull();
        assertThat(response.reply()).isEqualTo("grazie per la recensione");
    }

    @Test
    void reconcileAllRatings_recomputesEveryDistinctItem() {
        when(reviewRepository.findDistinctCatalogItemIds()).thenReturn(List.of(1L, 2L, 3L));
        when(reviewRepository.findByCatalogItemId(anyLong())).thenReturn(List.of(review(1L, 4, "t")));

        service.reconcileAllRatings();

        verify(catalogClient).updateRating(eq(1L), eq("test-key"), any());
        verify(catalogClient).updateRating(eq(2L), eq("test-key"), any());
        verify(catalogClient).updateRating(eq(3L), eq("test-key"), any());
    }

    @Test
    void getReviewsByItem_mapsEntitiesToResponses() {
        when(reviewRepository.findByCatalogItemId(ITEM)).thenReturn(List.of(review(1L, 5, "a"), review(2L, 3, "b")));

        List<ReviewResponse> result = service.getReviewsByItem(ITEM);

        assertThat(result).extracting(ReviewResponse::rating).containsExactly(5, 3);
        assertThat(result).extracting(ReviewResponse::travelerId).containsExactly("a", "b");
    }

    @Test
    void getReviewsByTraveler_mapsEntitiesToResponses() {
        when(reviewRepository.findByTravelerId(TRAVELER)).thenReturn(List.of(review(1L, 4, TRAVELER)));

        List<ReviewResponse> result = service.getReviewsByTraveler(TRAVELER);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).travelerId()).isEqualTo(TRAVELER);
    }
}
