package com.tripify.booking_service.repository;

import com.tripify.booking_service.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface bookingRepository extends JpaRepository<Booking, Long>{

    List<Booking> findByUserIdOrderByBookingDateDesc(String userId);
}
