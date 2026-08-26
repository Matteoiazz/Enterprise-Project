package com.tripify.itinerary_service.exception;

public class PublishRequirementsNotMetException extends RuntimeException {
    public PublishRequirementsNotMetException(String message) {
        super(message);
    }
}
