package com.tripify.communication_service.controller;

import com.tripify.communication_service.entity.ChatMessage;
import com.tripify.communication_service.entity.ChatRoom;
import com.tripify.communication_service.repository.ChatMessageRepository;
import com.tripify.communication_service.repository.ChatRoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    // Metodo di servizio per estrarre l'UUID (sub) dell'utente dal token JWT di Keycloak
    private String extractUserId(Principal principal) {
        if (principal instanceof JwtAuthenticationToken) {
            Jwt jwt = ((JwtAuthenticationToken) principal).getToken();
            // Il subject ('sub') in Keycloak è l'UUID univoco dell'utente
            return jwt.getSubject();
        }
        // Fallback di sicurezza se per test il token non è presente o è un principal generico
        return principal != null ? principal.getName() : "anonymous";
    }

    // 1. APRE O CREA LA CHAT TRA VIAGGIATORE E HOST (Via REST)
    // Il travelerId NON arriva più dai parametri, ma viene estratto in modo sicuro dal token JWT!
    @PostMapping("/chat/room")
    @ResponseBody
    public ChatRoom getOrCreateChatRoom(@RequestParam String hostId, Principal principal) {
        String travelerId = extractUserId(principal);

        Optional<ChatRoom> existingRoom = chatRoomRepository.findByTravelerIdAndHostId(travelerId, hostId);
        if (existingRoom.isPresent()) {
            return existingRoom.get();
        }

        // Se non esiste, la creiamo al volo con gli UUID
        ChatRoom newRoom = new ChatRoom();
        newRoom.setTravelerId(travelerId);
        newRoom.setHostId(hostId);
        return chatRoomRepository.save(newRoom);
    }

    // 2. RECUPERA LA LISTA DELLE CHAT APERTE (Per la schermata Inbox)
    // Prende l'utente direttamente dal token, senza fargli passare l'ID nell'URL
    @GetMapping("/chat/rooms")
    @ResponseBody
    public List<ChatRoom> getUserChatRooms(Principal principal) {
        String userId = extractUserId(principal);

        List<ChatRoom> asTraveler = chatRoomRepository.findByTravelerId(userId);
        List<ChatRoom> asHost = chatRoomRepository.findByHostId(userId);
        asTraveler.addAll(asHost);
        return asTraveler;
    }

    // 3. RECUPERO CRONOLOGIA MESSAGGI DI UNA STANZA SPECIFICA
    @GetMapping("/chat/history/{roomId}")
    @ResponseBody
    public List<ChatMessage> getChatHistory(@PathVariable String roomId) {
        return chatMessageRepository.findByRoomIdOrderByTimestampAsc(roomId);
    }

    // 4. RICEZIONE E INVIO IN TEMPO REALE (WebSocket)
    @MessageMapping("/chat.sendMessage")
    public void processMessage(@Payload ChatMessage chatMessage, Principal principal) {
        // Anche sul WebSocket blindiamo il senderId prendendolo dal token di chi è connesso,
        // impedendo che un utente possa mandare messaggi spacciandosi per un altro.
        if (principal != null) {
            chatMessage.setSenderId(extractUserId(principal));
        }

        System.out.println("DEBUG: Messaggio ricevuto per la stanza " + chatMessage.getRoomId() +
                " da " + chatMessage.getSenderId() +
                ". Testo: " + chatMessage.getContent());

        // Salva il messaggio nel DB collegato alla stanza
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

        // Invia il messaggio in tempo reale a chi è in ascolto su quella stanza specifica
        messagingTemplate.convertAndSend("/topic/room/" + chatMessage.getRoomId(), savedMessage);
    }
}