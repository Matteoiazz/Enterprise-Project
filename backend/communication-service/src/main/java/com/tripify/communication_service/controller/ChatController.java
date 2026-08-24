package com.tripify.communication_service.controller;

import com.tripify.communication_service.entity.ChatMessage;
import com.tripify.communication_service.entity.ChatRoom;
import com.tripify.communication_service.repository.ChatMessageRepository;
import com.tripify.communication_service.repository.ChatRoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    // 1. APRE O CREA LA CHAT TRA VIAGGIATORE E HOST (Chiamata HTTP REST)
    // Chiamata da Android quando si clicca "Contatta organizzatore" dalla pagina del viaggio
    @PostMapping("/chat/room")
    @ResponseBody
    public ChatRoom getOrCreateChatRoom(@RequestParam Long travelerId, @RequestParam Long hostId) {
        Optional<ChatRoom> existingRoom = chatRoomRepository.findByTravelerIdAndHostId(travelerId, hostId);
        if (existingRoom.isPresent()) {
            return existingRoom.get();
        }
        // Se non esiste, la creiamo al volo
        ChatRoom newRoom = new ChatRoom();
        newRoom.setTravelerId(travelerId);
        newRoom.setHostId(hostId);
        return chatRoomRepository.save(newRoom);
    }

    // 2. RECUPERA LA LISTA DELLE CHAT APERTE (Per la schermata Inbox)
    @GetMapping("/chat/rooms/{userId}")
    @ResponseBody
    public List<ChatRoom> getUserChatRooms(@PathVariable Long userId) {
        // Restituisce le chat sia se l'utente è viaggiatore, sia se è host
        List<ChatRoom> asTraveler = chatRoomRepository.findByTravelerId(userId);
        List<ChatRoom> asHost = chatRoomRepository.findByHostId(userId);
        asTraveler.addAll(asHost);
        return asTraveler;
    }

    // 3. RECUPERO CRONOLOGIA MESSAGGI DI UNA STANZA SPECIFICA
    @GetMapping("/chat/history/{roomId}")
    @ResponseBody
    public List<ChatMessage> getChatHistory(@PathVariable Long roomId) {
        return chatMessageRepository.findByRoomIdOrderByTimestampAsc(roomId);
    }

    // 4. RICEZIONE E INVIO IN TEMPO REALE (WebSocket)
    @MessageMapping("/chat.sendMessage")
    public void processMessage(@Payload ChatMessage chatMessage) {
        System.out.println("DEBUG: Messaggio ricevuto per la stanza " + chatMessage.getRoomId() +
                " da " + chatMessage.getSenderId() +
                ". Testo: " + chatMessage.getContent());

        // Salva il messaggio nel DB collegato alla stanza
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

        // Invia il messaggio in tempo reale a chi è in ascolto su quella stanza specifica
        messagingTemplate.convertAndSend("/topic/room/" + chatMessage.getRoomId(), savedMessage);
    }
}