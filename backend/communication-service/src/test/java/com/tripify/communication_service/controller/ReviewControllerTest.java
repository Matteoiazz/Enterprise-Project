package com.tripify.communication_service.controller;

import com.tripify.communication_service.dto.CreateReviewRequest;
import com.tripify.communication_service.dto.ReplyReviewRequest;
import com.tripify.communication_service.dto.ReviewResponse;
import com.tripify.communication_service.dto.UpdateReviewRequest;
import com.tripify.communication_service.service.ReviewService;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock ReviewService reviewService;

    @InjectMocks ReviewController controller;

    private Jwt jwtWithSub(String sub) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(sub);
        return jwt;
    }

    private ReviewResponse sample() {
        return new ReviewResponse(1L, 5, "ottimo", "sub-1", 42L, null, null);
    }

    @Test
    void addReview_usesJwtSubjectAsTravelerId() {
        when(reviewService.createReview(5, "ottimo", "sub-1", 42L)).thenReturn(sample());

        ResponseEntity<ReviewResponse> response = controller.addReview(jwtWithSub("sub-1"),
                new CreateReviewRequest(5, "ottimo", 42L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().rating()).isEqualTo(5);
    }

    @Test
    void updateReview_delegatesWithPathIdAndSubject() {
        when(reviewService.updateReview(1L, 3, "meh", "sub-1")).thenReturn(sample());

        ResponseEntity<ReviewResponse> response = controller.updateReview(jwtWithSub("sub-1"), 1L,
                new UpdateReviewRequest(3, "meh"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(reviewService).updateReview(1L, 3, "meh", "sub-1");
    }

    @Test
    void deleteReview_returns204() {
        ResponseEntity<Void> response = controller.deleteReview(jwtWithSub("sub-1"), 1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(reviewService).deleteReview(1L, "sub-1");
    }

    @Test
    void replyToReview_delegatesReplyAndSubject() {
        ReviewResponse withReply = new ReviewResponse(1L, 5, "ottimo", "sub-1", 42L, "grazie", Instant.now());
        when(reviewService.replyToReview(1L, "grazie", "host-1")).thenReturn(withReply);

        ResponseEntity<ReviewResponse> response = controller.replyToReview(jwtWithSub("host-1"), 1L,
                new ReplyReviewRequest("grazie"));

        assertThat(response.getBody().reply()).isEqualTo("grazie");
    }

    @Test
    void getReviewsForItem_isNotRestrictedToTheCaller() {
        when(reviewService.getReviewsByItem(42L)).thenReturn(List.of(sample()));

        ResponseEntity<List<ReviewResponse>> response = controller.getReviewsForItem(42L);

        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getReviewsByTraveler_rejectsRequestForAnotherUser() {
        assertThatThrownBy(() -> controller.getReviewsByTraveler(jwtWithSub("sub-1"), "sub-2"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void handleBadRequest_maps400() {
        ResponseEntity<String> response = controller.handleBadRequest(new IllegalArgumentException("rating non valido"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("rating non valido");
    }

    @Test
    void handleForbidden_maps403() {
        ResponseEntity<String> response = controller.handleForbidden(new IllegalStateException("non tuo"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isEqualTo("non tuo");
    }

    @Test
    void handleNotFound_maps404() {
        ResponseEntity<String> response = controller.handleNotFound(new NoSuchElementException("Recensione non trovata"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("Recensione non trovata");
    }

    @Test
    void handleDownstreamUnavailable_maps503() {
        ResponseEntity<String> response = controller.handleDownstreamUnavailable(mock(FeignException.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void handleInvalidBody_joinsFieldMessages() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("dto", "rating", "obbligatorio"),
                new FieldError("dto", "comment", "vuoto")));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<String> response = controller.handleInvalidBody(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("obbligatorio").contains("vuoto");
    }
}
