package com.tripify.communication_service.controller;

import com.tripify.communication_service.entity.ChatMessage;
import com.tripify.communication_service.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    // 1. RICEZIONE E INVIO IN TEMPO REALE (WebSocket)
    // Android invierà i messaggi a: /app/chat.sendMessage
    @MessageMapping("/chat.sendMessage")
    public void processMessage(@Payload ChatMessage chatMessage) {

        // Salviamo il messaggio nel database (il timestamp si autogenera)
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

        // Inviamo il messaggio in tempo reale solo al destinatario!
        // Il destinatario (su Android) dovrà essere in "ascolto" su: /user/{suoId}/queue/messages
        messagingTemplate.convertAndSendToUser(
                String.valueOf(chatMessage.getReceiverId()),
                "/queue/messages",
                savedMessage
        );
    }

    // 2. RECUPERO CRONOLOGIA (Classica chiamata HTTP GET)
    // Android chiamerà questo endpoint quando un utente apre la schermata della chat
    @GetMapping("/chat/history/{user1Id}/{user2Id}")
    @ResponseBody
    public List<ChatMessage> getChatHistory(@PathVariable Long user1Id, @PathVariable Long user2Id) {
        return chatMessageRepository.findConversationHistory(user1Id, user2Id);
    }
}