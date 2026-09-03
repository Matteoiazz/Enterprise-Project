package com.tripify.itinerary_service.dto;

import java.time.LocalDate;

// Rispecchia esattamente AddToCartRequestDTO di booking-service (POST /api/v1/cart/add):
// è il corpo che ItineraryService.bookAllItems inoltra per ogni componente della lista.
public record AddToCartRequestDTO(
        Long catalogItemId,
        Integer quantity,
        Long roomTypeId,
        Long fareClassId,
        LocalDate checkIn,
        LocalDate checkOut
) {}
