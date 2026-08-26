package com.tripify.booking_service.dto;

// Rispecchia SeatHoldRequestDTO di catalog-service: usato solo come corpo
// in uscita verso AvailabilityController, non arriva mai da un client nostro.
public record SeatHoldRequestDTO(
        Integer seats,
        String userId
) {}
