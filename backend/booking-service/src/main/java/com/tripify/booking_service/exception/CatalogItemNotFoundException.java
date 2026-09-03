package com.tripify.booking_service.exception;

public class CatalogItemNotFoundException extends RuntimeException {
    public CatalogItemNotFoundException(String message) {
        super(message);
    }
}
