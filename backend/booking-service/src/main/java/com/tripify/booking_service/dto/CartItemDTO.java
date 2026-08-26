package com.tripify.booking_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CartItemDTO(
        Long id,
        Long catalogItemId,
        Integer quantity,
        BigDecimal priceAtAdded,
        Long roomTypeId,
        Long fareClassId,
        LocalDate checkIn,
        LocalDate checkOut
) {}
