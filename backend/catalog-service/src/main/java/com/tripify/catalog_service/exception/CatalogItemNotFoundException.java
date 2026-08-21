package com.tripify.catalog_service.exception;

public class CatalogItemNotFoundException extends RuntimeException {
    public CatalogItemNotFoundException(Long id) {
        super("Nessun elemento del catalogo trovato con id " + id);
    }
}
