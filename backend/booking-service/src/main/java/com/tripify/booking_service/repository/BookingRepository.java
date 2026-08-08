package com.tripify.booking_service.repository;

import com.tripify.booking_service.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>{

    // Il tuo metodo originale mantenuto intatto
    List<Booking> findByUserIdOrderByBookingDateDesc(String userId);

    // IL NUOVO METODO CHE RISOLVE L'ERRORE NEL SERVICE:
    // Cerca i viaggi dove l'utente è il creatore OPPURE è presente nella lista dei partecipanti
    List<Booking> findAllByUserIdOrParticipantIdsContaining(String userId, String participantId);
}