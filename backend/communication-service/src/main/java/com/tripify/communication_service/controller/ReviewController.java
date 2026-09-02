package com.tripify.communication_service.controller;

import com.tripify.communication_service.dto.CreateReviewRequest;
import com.tripify.communication_service.dto.ReplyReviewRequest;
import com.tripify.communication_service.dto.ReviewResponse;
import com.tripify.communication_service.dto.UpdateReviewRequest;
import com.tripify.communication_service.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> addReview(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateReviewRequest request) {

        String travelerId = jwt.getSubject();
        ReviewResponse savedReview = reviewService.createReview(
                request.rating(), request.comment(), travelerId, request.catalogItemId());
        return ResponseEntity.ok(savedReview);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> updateReview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody UpdateReviewRequest request) {

        String travelerId = jwt.getSubject();
        ReviewResponse updatedReview = reviewService.updateReview(id, request.rating(), request.comment(), travelerId);
        return ResponseEntity.ok(updatedReview);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        String travelerId = jwt.getSubject();
        reviewService.deleteReview(id, travelerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reply")
    public ResponseEntity<ReviewResponse> replyToReview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody ReplyReviewRequest request) {

        return ResponseEntity.ok(reviewService.replyToReview(id, request.reply(), jwt.getSubject()));
    }

    @PostMapping("/{id}/helpful")
    public ResponseEntity<ReviewResponse> toggleHelpful(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ResponseEntity.ok(reviewService.toggleHelpful(id, jwt.getSubject()));
    }

    @GetMapping("/item/{catalogItemId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsForItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long catalogItemId) {

        String callerId = jwt != null ? jwt.getSubject() : null;
        return ResponseEntity.ok(reviewService.getReviewsByItem(catalogItemId, callerId));
    }

    @GetMapping("/traveler/{travelerId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsByTraveler(@AuthenticationPrincipal Jwt jwt, @PathVariable String travelerId) {
        if (!jwt.getSubject().equals(travelerId)) {
            throw new IllegalStateException("Non autorizzato a visualizzare le recensioni di un altro utente");
        }
        return ResponseEntity.ok(reviewService.getReviewsByTraveler(travelerId));
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message == null ? status.getReasonPhrase() : message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidBody(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, message.isBlank() ? "Dati non validi" : message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(IllegalStateException e) {
        return error(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(java.util.NoSuchElementException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(feign.FeignException.class)
    public ResponseEntity<Map<String, Object>> handleDownstreamUnavailable(feign.FeignException e) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "Servizio temporaneamente non raggiungibile, riprova più tardi.");
    }
}
