package com.tripify.communication_service.repository;

import com.tripify.communication_service.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Trova tutti i messaggi di una specifica ChatRoom ordinati per data
    List<ChatMessage> findByRoomIdOrderByTimestampAsc(Long roomId);
}