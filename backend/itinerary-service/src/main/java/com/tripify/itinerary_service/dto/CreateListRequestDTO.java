package com.tripify.itinerary_service.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateListRequestDTO(@NotBlank(message = "il nome è obbligatorio") String name) {
}
