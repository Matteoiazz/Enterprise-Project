package com.tripify.itinerary_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record GenerateItineraryRequestDTO(
        @NotBlank(message = "la città di partenza è obbligatoria") String departureCity,
        @NotBlank(message = "la città è obbligatoria") String city,
        @Min(value = 1, message = "la durata minima è 1 giorno") @Max(value = 14, message = "la durata massima è 14 giorni") int days,
        @Min(value = 1, message = "servono almeno 1 viaggiatore") @Max(value = 20, message = "massimo 20 viaggiatori") int travelers,
        boolean returnFlight,
        @Positive(message = "il budget deve essere positivo") BigDecimal budget
) {
}
