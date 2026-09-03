package com.tripify.catalog_service.entity;

// Uno scadere non ha un proprio stato: un hold HELD il cui expiresAt e' nel
// passato viene trattato come scaduto confrontando la data (vedi le query di
// disponibilita' e FlightCleanupService), senza mai passare a un valore dedicato.
public enum HoldStatus {
    HELD,
    CONFIRMED,
    RELEASED
}
