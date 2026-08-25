package com.tripify.itinerary_service.dto;

import com.tripify.itinerary_service.entity.Visibility;
import jakarta.validation.constraints.NotNull;

public record UpdateVisibilityRequestDTO(
        @NotNull(message = "la visibilità è obbligatoria") Visibility visibility,
        String city
) {
}
