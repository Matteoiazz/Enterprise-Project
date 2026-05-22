package com.tripify.booking_service.repository;

import com.tripify.booking_service.entity.passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface passengerRepository extends JpaRepository<passenger, Long>{

    List<passenger> findByBookingLineId(Long bookingLineId);

    Optional<passenger> findByTaxCodeAndBookingLineId(String taxCode, Long bookingLineId);
}
