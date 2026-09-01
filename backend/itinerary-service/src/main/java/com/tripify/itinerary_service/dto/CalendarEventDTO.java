package com.tripify.itinerary_service.dto;

import java.time.LocalDateTime;

/**
 * Un singolo evento da scrivere nel file .ics esportato (vedi IcsBuilder).
 * allDay=true per hotel/attività (solo la data conta, niente orario), false
 * per i voli (orario reale di partenza/arrivo).
 */
public record CalendarEventDTO(
        String summary,
        String location,
        LocalDateTime start,
        LocalDateTime end,
        boolean allDay
) {}
