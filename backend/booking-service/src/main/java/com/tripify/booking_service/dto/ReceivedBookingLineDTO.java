package com.tripify.booking_service.dto;

import com.tripify.booking_service.entity.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Una riga di prenotazione fatta da un ALTRO utente su un annuncio di chi chiama
// (vedi BookingService.getReceivedBookings): a differenza di BookingLineDTO, qui
// serve sapere anche chi ha acquistato (buyerUserId) e lo stato/data della
// Booking a cui la riga appartiene, non i dettagli interni della prenotazione.
public record ReceivedBookingLineDTO(
        Long bookingId,
        String buyerUserId,
        Long catalogItemId,
        Integer quantity,
        BigDecimal price,
        LocalDate checkIn,
        LocalDate checkOut,
        BookingStatus status,
        LocalDateTime bookingDate
) {
}
