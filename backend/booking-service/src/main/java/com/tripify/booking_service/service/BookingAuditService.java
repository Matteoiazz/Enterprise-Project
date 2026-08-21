package com.tripify.booking_service.service;

import com.tripify.booking_service.entity.AuditAction;
import com.tripify.booking_service.entity.Booking;
import com.tripify.booking_service.entity.BookingAuditLog;
import com.tripify.booking_service.repository.BookingAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

// Service separato con un'unica responsabilità: leggere/scrivere eventi di audit.
// Tenerlo distinto da BookingService evita di mescolare la logica di business
// con quella di tracciamento, e lo rende riusabile se in futuro altre azioni
// sulla Booking (es. cancellazione, check-in) dovranno anch'esse loggare.
@Service
@RequiredArgsConstructor
public class BookingAuditService {

    private final BookingAuditLogRepository auditLogRepository;

    public void log(Booking booking, String performedBy, AuditAction action, String details) {
        BookingAuditLog entry = BookingAuditLog.builder()
                .booking(booking)
                .performedBy(performedBy)
                .action(action)
                .details(details)
                .build();

        auditLogRepository.save(entry);
    }

    public List<BookingAuditLog> getHistory(Long bookingId) {
        return auditLogRepository.findByBookingIdOrderByCreatedAtAsc(bookingId);
    }
}