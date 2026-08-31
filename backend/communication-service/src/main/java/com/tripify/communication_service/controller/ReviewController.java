package com.tripify.communication_service.controller;

import com.tripify.communication_service.entity.Review;
import com.tripify.communication_service.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<Review> addReview(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam Integer rating,
            @RequestParam String comment,
            @RequestParam Long catalogItemId) {

        String travelerId = jwt.getSubject();
        Review savedReview = reviewService.createReview(rating, comment, travelerId, catalogItemId);
        return ResponseEntity.ok(savedReview);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Review> updateReview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestParam Integer rating,
            @RequestParam String comment) {

        String travelerId = jwt.getSubject();
        Review updatedReview = reviewService.updateReview(id, rating, comment, travelerId);
        return ResponseEntity.ok(updatedReview);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        String travelerId = jwt.getSubject();
        reviewService.deleteReview(id, travelerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/item/{catalogItemId}")
    public ResponseEntity<List<Review>> getReviewsForItem(@PathVariable Long catalogItemId) {
        return ResponseEntity.ok(reviewService.getReviewsByItem(catalogItemId));
    }

    @GetMapping("/traveler/{travelerId}")
    public ResponseEntity<List<Review>> getReviewsByTraveler(@PathVariable String travelerId) {
        return ResponseEntity.ok(reviewService.getReviewsByTraveler(travelerId));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleForbidden(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<String> handleNotFound(java.util.NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
}
