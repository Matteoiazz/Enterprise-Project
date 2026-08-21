package com.tripify.catalog_service.exception;

public class InvalidHoldStateException extends RuntimeException {
    public InvalidHoldStateException(String message) {
        super(message);
    }
}
