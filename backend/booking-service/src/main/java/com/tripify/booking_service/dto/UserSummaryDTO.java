package com.tripify.booking_service.dto;

// Solo per verificare che un utente esista (vedi BookingService.inviteFriend);
// ignoriamo il resto dei campi che l'endpoint restituisce.
public record UserSummaryDTO(String id) {}
