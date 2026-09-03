package com.tripify.booking_service.dto;

import java.math.BigDecimal;
import java.util.List;

// Rispecchia solo i campi che ci servono da CatalogItemDTO di catalog-service
// (Jackson ignora il resto): serve a calcolare il prezzo reale dell'articolo
// (tariffa/camera scelta) invece del solo prezzo base, vedi
// ShoppingCartService.addItem.
public record CatalogItemSummaryDTO(
        Long id,
        String itemType,
        BigDecimal price,
        String currency,
        List<RoomTypeSummaryDTO> roomTypes,
        List<FareClassSummaryDTO> fareClasses
) {
    public record RoomTypeSummaryDTO(Long id, BigDecimal price) {}
    public record FareClassSummaryDTO(Long id, BigDecimal price) {}
}
