package com.tripify.itinerary_service.dto;

// Rispecchia solo i campi che ci servono da CatalogItemDTO (Jackson ignora il resto).
public record CatalogItemSummaryDTO(Long id, String itemType, String title) {
}
