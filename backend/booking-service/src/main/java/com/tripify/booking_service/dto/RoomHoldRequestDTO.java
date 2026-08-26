package com.tripify.booking_service.dto;

import java.time.LocalDate;

// Rispecchia RoomHoldRequestDTO di catalog-service: usato solo come corpo
// in uscita verso AvailabilityController, non arriva mai da un client nostro.
public record RoomHoldRequestDTO(
        LocalDate checkIn,
        LocalDate checkOut,
        Integer rooms,
        String userId
) {}
