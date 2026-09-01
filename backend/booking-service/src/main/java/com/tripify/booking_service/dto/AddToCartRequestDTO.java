package com.tripify.booking_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

// Record: stesso pattern già usato per PaymentRequestDTO, dati in ingresso immutabili.
// roomTypeId/fareClassId/checkIn/checkOut sono opzionali: valorizzati solo quando
// l'articolo è rispettivamente una camera d'hotel o un posto su un volo, per
// aprire il relativo hold su catalog-service (vedi ShoppingCartService.addItem).
public record AddToCartRequestDTO(
        @NotNull(message = "catalogItemId è obbligatorio") Long catalogItemId,
        @NotNull(message = "la quantità è obbligatoria")
        @Min(value = 1, message = "la quantità deve essere almeno 1")
        @Max(value = 20, message = "la quantità non può superare 20 per singola aggiunta")
        Integer quantity,
        Long roomTypeId,
        Long fareClassId,
        LocalDate checkIn,
        LocalDate checkOut
) {}
