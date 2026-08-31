package com.tripify.communication_service.dto;

import com.tripify.communication_service.entity.Review;

// Vista esposta di una recensione: non serializziamo direttamente l'entità JPA.
// travelerId serve al client per riconoscere le proprie recensioni (modifica/
// eliminazione); gli altri campi dell'entità restano interni.
public record ReviewResponse(
        Long id,
        Integer rating,
        String comment,
        String travelerId,
        Long catalogItemId
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getRating(),
                review.getComment(),
                review.getTravelerId(),
                review.getCatalogItemId()
        );
    }
}
