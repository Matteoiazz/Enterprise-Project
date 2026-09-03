package com.tripify.itinerary_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Rispecchia solo i campi che ci servono da CatalogItemDTO (Jackson ignora il resto).
// departureCity/arrivalCity/departureTime/arrivalTime sono valorizzati solo per i
// voli, city solo per hotel/attività: servono a ItineraryService per validare la
// coerenza geografica/temporale di un itinerario (vedi validateItineraryCoherence).
public record CatalogItemSummaryDTO(
        Long id,
        String itemType,
        String title,
        BigDecimal price,
        String city,
        String departureCity,
        String arrivalCity,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        List<RoomTypeSummaryDTO> roomTypes,
        List<FareClassSummaryDTO> fareClasses
) {
    public record RoomTypeSummaryDTO(Long id, BigDecimal price, Integer maxOccupancy) {}
    public record FareClassSummaryDTO(Long id, BigDecimal price, Integer totalSeats) {}
}
