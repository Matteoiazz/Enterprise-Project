package com.tripify.itinerary_service.exception;

public class ListNotFoundException extends RuntimeException {
    public ListNotFoundException(Long listId) {
        super("Lista non trovata: " + listId);
    }

    public ListNotFoundException(String message) {
        super(message);
    }
}
