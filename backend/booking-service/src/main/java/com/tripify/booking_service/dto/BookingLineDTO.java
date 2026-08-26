package com.tripify.booking_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BookingLineDTO(
        Long id,
        Long catalogItemId,
        BigDecimal price,
        Integer quantity,
        Long roomTypeId,
        Long fareClassId,
        LocalDate checkIn,
        LocalDate checkOut,
        int passengerCount
) {}
