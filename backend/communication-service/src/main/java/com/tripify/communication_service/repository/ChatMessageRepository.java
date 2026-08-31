package com.tripify.communication_service.repository;

import com.tripify.communication_service.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByRoomIdOrderByTimestampAsc(String roomId);

    // Conta i messaggi non letti in una stanza, escludendo quelli inviati da te
    int countByRoomIdAndSenderIdNotAndIsReadFalse(String roomId, String senderId);

    // Aggiorna lo stato dei messaggi a 'letti' (solo quelli inviati dall'altro utente)
    @Modifying
    @Transactional
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.roomId = :roomId AND m.senderId != :userId AND m.isRead = false")
    void markAsReadByRoomAndRecipient(@Param("roomId") String roomId, @Param("userId") String userId);
}