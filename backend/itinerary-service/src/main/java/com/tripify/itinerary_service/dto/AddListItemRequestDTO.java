package com.tripify.itinerary_service.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

// roomTypeId/fareClassId/checkIn/checkOut vanno valorizzati solo quando il
// componente aggiunto è rispettivamente una camera d'hotel o un posto su un volo
// (servono a booking-service per aprire l'hold in fase di "prenota tutto").
public record AddListItemRequestDTO(
        @NotNull(message = "catalogItemId è obbligatorio") Long catalogItemId,
        Integer quantity,
        Long roomTypeId,
        Long fareClassId,
        LocalDate checkIn,
        LocalDate checkOut,
        LocalDate activityDate
) {}
