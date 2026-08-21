package com.tripify.catalog_service.repository;

import com.tripify.catalog_service.entity.RoomHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RoomHoldRepository extends JpaRepository<RoomHold, Long> {


    @Query("""
        SELECT h FROM RoomHold h
        WHERE h.roomType.id = :roomTypeId
          AND h.checkIn < :checkOut AND h.checkOut > :checkIn
          AND (h.status = com.tripify.catalog_service.entity.HoldStatus.CONFIRMED
               OR (h.status = com.tripify.catalog_service.entity.HoldStatus.HELD AND h.expiresAt > :now))
        """)
    List<RoomHold> findActiveOverlapping(
            @Param("roomTypeId") Long roomTypeId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("now") LocalDateTime now
    );
}
