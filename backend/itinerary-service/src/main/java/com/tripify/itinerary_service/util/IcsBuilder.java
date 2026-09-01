package com.tripify.itinerary_service.util;

import com.tripify.itinerary_service.dto.CalendarEventDTO;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Genera un file .ics (RFC 5545) da una lista di eventi, per l'esportazione
 * di un itinerario nel calendario del telefono. Gli orari dei voli sono
 * "floating" (senza Z né TZID): l'ora scritta è quella reale della partenza,
 * mostrata così com'è indipendentemente dal fuso di chi importa il file. Gli
 * eventi hotel/attività sono "all-day" (solo la data, nessun orario).
 */
public final class IcsBuilder {

    private static final DateTimeFormatter FLOATING = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter UTC_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    private IcsBuilder() {
    }

    public static String build(String calendarName, List<CalendarEventDTO> events) {
        StringBuilder sb = new StringBuilder();
        line(sb, "BEGIN:VCALENDAR");
        line(sb, "VERSION:2.0");
        line(sb, "PRODID:-//Tripify//Itinerario//IT");
        line(sb, "CALSCALE:GREGORIAN");
        line(sb, "X-WR-CALNAME:" + escape(calendarName));

        String stamp = LocalDateTime.now(ZoneOffset.UTC).format(UTC_STAMP);
        for (CalendarEventDTO event : events) {
            line(sb, "BEGIN:VEVENT");
            line(sb, "UID:" + UUID.randomUUID() + "@tripify.app");
            line(sb, "DTSTAMP:" + stamp);
            if (event.allDay()) {
                line(sb, "DTSTART;VALUE=DATE:" + event.start().format(DATE_ONLY));
                line(sb, "DTEND;VALUE=DATE:" + event.end().format(DATE_ONLY));
            } else {
                line(sb, "DTSTART:" + event.start().format(FLOATING));
                line(sb, "DTEND:" + event.end().format(FLOATING));
            }
            line(sb, "SUMMARY:" + escape(event.summary()));
            if (event.location() != null && !event.location().isBlank()) {
                line(sb, "LOCATION:" + escape(event.location()));
            }
            line(sb, "END:VEVENT");
        }

        line(sb, "END:VCALENDAR");
        return sb.toString();
    }

    private static void line(StringBuilder sb, String content) {
        sb.append(content).append("\r\n");
    }

    /** Escaping testo per campi iCalendar (RFC 5545 §3.3.11): backslash, virgola, punto e virgola, a capo. */
    private static String escape(String text) {
        return text
                .replace("\\", "\\\\")
                .replace(",", "\\,")
                .replace(";", "\\;")
                .replace("\n", "\\n");
    }
}
