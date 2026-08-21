package com.tripify.booking_service.dto;

import java.math.BigDecimal;

// Record: immutabile, niente boilerplate di getter/setter per un DTO che non deve mai cambiare stato
public record PaymentRequestDTO(
        Long bookingId,
        String cardNumber,
        BigDecimal amount
) {}