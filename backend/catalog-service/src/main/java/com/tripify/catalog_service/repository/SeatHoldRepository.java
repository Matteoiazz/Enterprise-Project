package com.tripify.catalog_service.repository;

import com.tripify.catalog_service.entity.SeatHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface SeatHoldRepository extends JpaRepository<SeatHold, Long> {

    @Query("""
        SELECT COALESCE(SUM(h.seats), 0) FROM SeatHold h
        WHERE h.fareClass.id = :fareClassId
          AND (h.status = com.tripify.catalog_service.entity.HoldStatus.CONFIRMED
               OR (h.status = com.tripify.catalog_service.entity.HoldStatus.HELD AND h.expiresAt > :now))
        """)
    Integer sumActiveSeats(@Param("fareClassId") Long fareClassId, @Param("now") LocalDateTime now);
}
