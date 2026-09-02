package com.tripify.itinerary_service.util;

import com.tripify.itinerary_service.dto.CalendarEventDTO;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
            line(sb, "UID:" + event.uid() + "@tripify.app");
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

    private static final int MAX_LINE_OCTETS = 75;

    private static void line(StringBuilder sb, String content) {
        sb.append(fold(content)).append("\r\n");
    }

    /**
     * Line folding (RFC 5545 §3.1): nessuna riga di contenuto puo' superare i 75 ottetti
     * (non caratteri: un accento in UTF-8 pesa piu' di un ottetto). Le righe piu' lunghe
     * vengono spezzate con CRLF seguito da un singolo spazio, che chi legge deve ignorare
     * in fase di ricomposizione.
     */
    private static String fold(String content) {
        if (content.getBytes(StandardCharsets.UTF_8).length <= MAX_LINE_OCTETS) {
            return content;
        }
        StringBuilder folded = new StringBuilder();
        int lineOctets = 0;
        int budget = MAX_LINE_OCTETS;
        int i = 0;
        while (i < content.length()) {
            int codePoint = content.codePointAt(i);
            int charCount = Character.charCount(codePoint);
            int codePointOctets = new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8).length;
            if (lineOctets + codePointOctets > budget) {
                folded.append("\r\n ");
                lineOctets = 0;
                budget = MAX_LINE_OCTETS - 1; // la riga di continuazione inizia gia' con uno spazio
            }
            folded.appendCodePoint(codePoint);
            lineOctets += codePointOctets;
            i += charCount;
        }
        return folded.toString();
    }

    /** Escaping testo per campi iCalendar (RFC 5545 §3.3.11): backslash, virgola, punto e virgola, a capo. */
    private static String escape(String text) {
        return text
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replace("\\", "\\\\")
                .replace(",", "\\,")
                .replace(";", "\\;")
                .replace("\n", "\\n");
    }
}
