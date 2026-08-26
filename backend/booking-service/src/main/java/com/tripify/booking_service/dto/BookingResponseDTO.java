package com.tripify.booking_service.dto;

import com.tripify.booking_service.entity.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BookingResponseDTO(
        Long id,
        BigDecimal totalAmount,
        LocalDateTime bookingDate,
        BookingStatus status,

        // IL CAMPO MAGICO PER IL FRONTEND
        boolean isLeader,

        List<String> participantIds,
        List<BookingLineDTO> lines
) {}
