package com.tripify.booking_service.repository;

import com.tripify.booking_service.entity.Booking;
import com.tripify.booking_service.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>{

    Page<Booking> findAllByUserIdOrParticipantIdsContaining(String userId, String participantId, Pageable pageable);

    List<Booking> findDistinctByLines_CatalogItemIdIn(List<Long> catalogItemIds);

    boolean existsByUserIdAndLines_CatalogItemIdAndStatus(String userId, Long catalogItemId, BookingStatus status);
}