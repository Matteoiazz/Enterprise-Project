package com.tripify.booking_service.entity;

public enum AuditAction {
    CREATED,
    PARTICIPANT_ADDED,
    PASSENGER_ADDED,
    STATUS_CHANGED
    // Aggiungi qui nuovi valori quando introdurrai altre operazioni sulla Booking
    // (es. CANCELLED, CHECKED_IN...) man mano che le implementi.
}