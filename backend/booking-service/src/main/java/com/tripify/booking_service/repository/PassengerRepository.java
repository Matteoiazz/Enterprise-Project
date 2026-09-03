package com.tripify.booking_service.repository;

import com.tripify.booking_service.entity.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PassengerRepository extends JpaRepository<Passenger, Long>{

    List<Passenger> findByBookingLineId(Long bookingLineId);

    Optional<Passenger> findByTaxCodeAndBookingLineId(String taxCode, Long bookingLineId);

    // Passeggeri di prenotazioni CONFERMATE il cui QR non è ancora stato
    // generato: gli hotel aprono il check-in quando la data di check-in è
    // entro "threshold" (di norma domani, cioè 24h prima), mentre voli/attività
    // non hanno una data di check-in propria (checkIn è null su quella riga) e
    // quindi il check-in si apre subito dopo la conferma del pagamento.
    @Query("SELECT p FROM Passenger p WHERE p.qrCodeData IS NULL " +
            "AND p.bookingLine.booking.status = com.tripify.booking_service.entity.BookingStatus.CONFIRMED " +
            "AND (p.bookingLine.checkIn IS NULL OR p.bookingLine.checkIn <= :threshold)")
    List<Passenger> findEligibleForCheckIn(@Param("threshold") LocalDate threshold);
}
