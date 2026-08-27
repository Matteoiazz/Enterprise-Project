package com.tripify.communication_service.repository;

import com.tripify.communication_service.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {

    // Trova la chat specifica tra un viaggiatore e un host
    Optional<ChatRoom> findByTravelerIdAndHostId(String travelerId, String hostId);

    // Trova tutte le chat aperte da un determinato viaggiatore (per la schermata Inbox)
    List<ChatRoom> findByTravelerId(String travelerId);

    // Trova tutte le chat in cui un utente partecipa come host
    List<ChatRoom> findByHostId(String hostId);
}