package com.tripify.itinerary_service.dto;

import java.time.LocalDateTime;

/**
 * Un singolo evento da scrivere nel file .ics esportato (vedi IcsBuilder).
 * allDay=true per hotel/attività (solo la data conta, niente orario), false
 * per i voli (orario reale di partenza/arrivo). uid è deterministico (lista +
 * posizione della tappa, vedi ItineraryService.exportToIcs): riesportando lo
 * stesso itinerario invariato, il calendario aggiorna gli eventi già importati
 * invece di duplicarli.
 */
public record CalendarEventDTO(
        String uid,
        String summary,
        String location,
        LocalDateTime start,
        LocalDateTime end,
        boolean allDay
) {}
