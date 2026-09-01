package com.tripify.communication_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReplyReviewRequest(
        @NotBlank(message = "La risposta non può essere vuota")
        @Size(max = 1000, message = "La risposta non può superare i 1000 caratteri")
        String reply
) {}
