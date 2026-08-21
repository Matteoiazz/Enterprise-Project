package com.tripify.catalog_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SeatHoldRequestDTO(
        @NotNull(message = "il numero di posti è obbligatorio") @Min(value = 1, message = "almeno 1 posto") Integer seats,
        @NotBlank(message = "userId è obbligatorio") String userId
) {
}
