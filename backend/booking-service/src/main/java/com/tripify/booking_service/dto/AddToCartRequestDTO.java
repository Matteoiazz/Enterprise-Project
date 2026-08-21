package com.tripify.booking_service.dto;

// Record: stesso pattern già usato per PaymentRequestDTO, dati in ingresso immutabili
public record AddToCartRequestDTO(
        Long catalogItemId,
        Integer quantity
) {}