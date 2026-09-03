package com.tripify.catalog_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RoomHoldRequestDTO(
        @NotNull(message = "checkIn è obbligatorio") LocalDate checkIn,
        @NotNull(message = "checkOut è obbligatorio") LocalDate checkOut,
        @NotNull(message = "il numero di camere è obbligatorio") @Min(value = 1, message = "almeno 1 camera") Integer rooms
) {
}
