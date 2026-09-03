package com.tripify.communication_service.dto;

import com.tripify.communication_service.entity.Review;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        Integer rating,
        String comment,
        String travelerId,
        Long catalogItemId,
        String reply,
        Instant repliedAt,
        int helpfulCount,
        boolean helpfulByMe,
        String travelerName
) {
    public static ReviewResponse from(Review review) {
        return from(review, 0, false);
    }

    public static ReviewResponse from(Review review, int helpfulCount, boolean helpfulByMe) {
        return new ReviewResponse(
                review.getId(),
                review.getRating(),
                review.getComment(),
                review.getTravelerId(),
                review.getCatalogItemId(),
                review.getReply(),
                review.getRepliedAt(),
                helpfulCount,
                helpfulByMe,
                review.getTravelerName()
        );
    }
}
