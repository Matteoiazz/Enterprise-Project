package com.tripify.booking_service.repository;

import com.tripify.booking_service.entity.BookingAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingAuditLogRepository extends JpaRepository<BookingAuditLog, Long> {

    // Storico eventi di una prenotazione, dal più vecchio al più recente
    List<BookingAuditLog> findByBookingIdOrderByCreatedAtAsc(Long bookingId);
}