package com.tripify.booking_service.repository;

import com.tripify.booking_service.entity.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PassengerRepository extends JpaRepository<Passenger, Long>{

    List<Passenger> findByBookingLineId(Long bookingLineId);

    Optional<Passenger> findByTaxCodeAndBookingLineId(String taxCode, Long bookingLineId);
}
