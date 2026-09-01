package com.tripify.booking_service.repository;

import com.tripify.booking_service.entity.Booking;
import com.tripify.booking_service.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>{

    // "member of" su participantIds (@ElementCollection) genera una subquery
    // EXISTS, non un JOIN nella query principale: a differenza della vecchia
    // findAllByUserIdOrParticipantIdsContaining (derivata dal nome, che univa
    // booking_participants con un JOIN), una Booking con più partecipanti non
    // compare più duplicata nei risultati, e la paginazione (LIMIT/OFFSET) resta
    // affidabile perché ogni riga della pagina corrisponde a UNA sola Booking.
    // @EntityGraph su "lines" evita anche l'N+1 di un accesso lazy per booking
    // in toResponseDTO (una sola query invece di una per riga di risultato).
    @EntityGraph(attributePaths = "lines")
    @Query("select b from Booking b where b.userId = :userId or :userId member of b.participantIds")
    Page<Booking> findVisibleToUser(@Param("userId") String userId, Pageable pageable);

    List<Booking> findDistinctByLines_CatalogItemIdIn(List<Long> catalogItemIds);

    boolean existsByUserIdAndLines_CatalogItemIdAndStatus(String userId, Long catalogItemId, BookingStatus status);

    // Usata per il replay idempotente del checkout (vedi BookingService.checkout).
    Optional<Booking> findByIdempotencyKey(String idempotencyKey);

    // "lines" caricate nella stessa query (JOIN FETCH): usata da confirmPayment
    // per leggere gli hold da confermare su catalog-service FUORI da una
    // transazione (vedi BookingService.confirmPayment) - senza questo, accedere
    // a booking.getLines() a transazione già chiusa lancerebbe
    // LazyInitializationException (open-in-view è disattivato).
    @Query("select distinct b from Booking b left join fetch b.lines where b.id = :id")
    Optional<Booking> findByIdWithLines(Long id);
}