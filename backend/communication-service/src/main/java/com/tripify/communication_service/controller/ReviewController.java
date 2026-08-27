package com.tripify.communication_service.controller;

import com.tripify.communication_service.entity.Review;
import com.tripify.communication_service.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // POST http://localhost:8084/api/reviews
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

    // GET http://localhost:8084/api/reviews/item/5
    @GetMapping("/item/{catalogItemId}")
    public ResponseEntity<List<Review>> getReviewsForItem(@PathVariable Long catalogItemId) {
        return ResponseEntity.ok(reviewService.getReviewsByItem(catalogItemId));
    }

    // GET http://localhost:8084/api/reviews/traveler/f8a9...
    @GetMapping("/traveler/{travelerId}")
    public ResponseEntity<List<Review>> getReviewsByTraveler(@PathVariable String travelerId) {
        return ResponseEntity.ok(reviewService.getReviewsByTraveler(travelerId));
    }
}