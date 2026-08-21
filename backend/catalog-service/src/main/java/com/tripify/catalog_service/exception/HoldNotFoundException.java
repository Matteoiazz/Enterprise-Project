package com.tripify.catalog_service.exception;

public class HoldNotFoundException extends RuntimeException {
    public HoldNotFoundException(String holdId) {
        super("Nessun blocco trovato con id " + holdId);
    }
}
