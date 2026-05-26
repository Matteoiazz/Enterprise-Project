package com.tripify.booking_service.repository;

import com.tripify.booking_service.entity.BookingLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingLineRepository extends JpaRepository<BookingLine, Long>{
}
