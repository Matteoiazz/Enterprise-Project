package com.tripify.booking_service.dto;

// Usato solo per verificare che un utente esista (vedi BookingService.inviteFriend):
// non ci serve nient'altro del suo profilo, quindi ignoriamo il resto dei campi
// che user-auth-service restituisce su GET /profile/users/{id}/summary.
public record UserSummaryDTO(String id) {}
