package com.tripify.catalog_service.repository;

import com.tripify.catalog_service.entity.HoldStatus;
import com.tripify.catalog_service.entity.SeatHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SeatHoldRepository extends JpaRepository<SeatHold, Long> {

    @Query("""
        SELECT COALESCE(SUM(h.seats), 0) FROM SeatHold h
        WHERE h.fareClass.id = :fareClassId
          AND (h.status = com.tripify.catalog_service.entity.HoldStatus.CONFIRMED
               OR (h.status = com.tripify.catalog_service.entity.HoldStatus.HELD AND h.expiresAt > :now))
        """)
    Integer sumActiveSeats(@Param("fareClassId") Long fareClassId, @Param("now") LocalDateTime now);

    // Rimuove solo gli hold non confermati (HELD/RELEASED/EXPIRED) sui voli già
    // partiti: prerequisito per poter cancellare le loro fare_classes senza violare
    // il vincolo di chiave esterna (vedi FlightCleanupService). Gli hold CONFIRMED
    // restano intatti: sono lo storico di una prenotazione reale.
    void deleteByFareClass_Flight_DepartureTimeBeforeAndStatusNot(LocalDateTime time, HoldStatus status);

    // Id dei voli già partiti che hanno ancora almeno un hold CONFIRMED: quel volo non
    // va cancellato, altrimenti la cascade su fare_classes romperebbe la FK dell'hold
    // (o lo cancellerebbe, perdendo lo storico della prenotazione).
    @Query("""
        SELECT DISTINCT h.fareClass.flight.id FROM SeatHold h
        WHERE h.status = com.tripify.catalog_service.entity.HoldStatus.CONFIRMED
          AND h.fareClass.flight.departureTime < :time
        """)
    List<Long> findFlightIdsWithConfirmedHoldBefore(@Param("time") LocalDateTime time);
}
