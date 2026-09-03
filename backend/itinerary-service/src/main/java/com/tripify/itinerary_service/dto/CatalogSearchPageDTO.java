package com.tripify.itinerary_service.dto;

import java.util.List;

// Rispecchia solo il campo "content" della Page<CatalogItemDTO> restituita da
// GET /items/search: Jackson ignora gli altri campi di paginazione, non ci servono.
public record CatalogSearchPageDTO(List<CatalogItemSummaryDTO> content) {
}
