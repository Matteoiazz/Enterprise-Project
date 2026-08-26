package com.tripify.booking_service.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartDTO(
        Long id,
        List<CartItemDTO> items,
        BigDecimal totalAmount
) {}
