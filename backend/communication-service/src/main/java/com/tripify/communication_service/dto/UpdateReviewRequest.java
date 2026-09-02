package com.tripify.communication_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateReviewRequest(
        @NotNull(message = "Il rating è obbligatorio")
        @Min(value = 1, message = "Il rating deve essere compreso tra 1 e 5")
        @Max(value = 5, message = "Il rating deve essere compreso tra 1 e 5")
        Integer rating,

        @NotBlank(message = "Il commento non può essere vuoto")
        @Size(max = 1000, message = "Il commento non può superare i 1000 caratteri")
        String comment,

        Boolean showName
) {}
