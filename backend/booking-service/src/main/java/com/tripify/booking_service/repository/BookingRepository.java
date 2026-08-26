package com.tripify.booking_service.repository;

import com.tripify.booking_service.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>{

    // Cerca i viaggi dove l'utente è il creatore OPPURE è presente nella lista
    // dei partecipanti, paginato per non far crescere la risposta senza limiti
    // man mano che l'utente accumula prenotazioni.
    Page<Booking> findAllByUserIdOrParticipantIdsContaining(String userId, String participantId, Pageable pageable);
}
