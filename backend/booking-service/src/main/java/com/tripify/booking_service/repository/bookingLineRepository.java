package com.tripify.booking_service.repository;

import com.tripify.booking_service.entity.bookingLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface bookingLineRepository extends JpaRepository<bookingLine, Long>{
}
