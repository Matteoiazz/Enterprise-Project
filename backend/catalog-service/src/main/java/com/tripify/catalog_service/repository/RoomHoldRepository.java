package com.tripify.catalog_service.repository;

import com.tripify.catalog_service.entity.HoldStatus;
import com.tripify.catalog_service.entity.RoomHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomHoldRepository extends JpaRepository<RoomHold, Long> {

    // Stesso lock pessimistico usato per room_types/fare_classes in holdRoom/holdSeats:
    // evita che due confirm/release concorrenti sullo stesso hold leggano lo stato
    // prima che l'altra li scriva, superando entrambi il controllo in nextStatusOn*.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM RoomHold h WHERE h.id = :id")
    Optional<RoomHold> findByIdForUpdate(@Param("id") Long id);

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

    // A differenza dei voli, un hotel resta prenotabile per sempre (nuove date future):
    // qui si ripuliscono solo gli hold ormai storici, mai l'hotel/la RoomType stessa.
    // Gli hold CONFIRMED restano intatti (storico di un soggiorno reale).
    int deleteByCheckOutBeforeAndStatusNot(LocalDate checkOut, HoldStatus status);
}
