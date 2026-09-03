package com.tripify.booking_service.dto;

import com.tripify.booking_service.entity.BookingStatus;

public record PaymentResultDTO(
        boolean success,
        String message,
        Long bookingId,
        BookingStatus bookingStatus
) {}
