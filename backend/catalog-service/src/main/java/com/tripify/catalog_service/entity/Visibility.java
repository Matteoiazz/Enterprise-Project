package com.tripify.catalog_service.entity;

public enum Visibility {
    PRIVATE,   // Visibile solo al creatore
    SHARED,    // Visibile a specifici ID utente
    PUBLIC     // Visibile a chiunque tramite link (Capabilities)
}