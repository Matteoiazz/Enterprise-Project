package com.tripify.communication_service.controller;

import com.tripify.communication_service.config.RabbitMQConfig; // Importa la config RabbitMQ
import com.tripify.communication_service.entity.ChatMessage;
import com.tripify.communication_service.entity.ChatRoom;
import com.tripify.communication_service.messaging.NotificationEvent; // Importa l'evento
import com.tripify.communication_service.repository.ChatMessageRepository;
import com.tripify.communication_service.repository.ChatRoomRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate; // Importa il RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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

    @Autowired
    private RabbitTemplate rabbitTemplate; // <--- AGGIUNTO PER LE NOTIFICHE

    private String extractUserId(Principal principal) {
        if (principal == null) {
            return "anonymousUser";
        }
        if (principal instanceof JwtAuthenticationToken jwtToken) {
            return jwtToken.getToken().getSubject();
        }
        return principal.getName();
    }

    @PostMapping("/chat/room")
    @ResponseBody
    public ChatRoom getOrCreateChatRoom(@RequestParam String hostId, Principal principal) {
        String travelerId = extractUserId(principal);

        Optional<ChatRoom> existingRoom = chatRoomRepository.findByTravelerIdAndHostId(travelerId, hostId);
        if (existingRoom.isPresent()) {
            return existingRoom.get();
        }

        ChatRoom newRoom = new ChatRoom();
        newRoom.setTravelerId(travelerId);
        newRoom.setHostId(hostId);
        return chatRoomRepository.save(newRoom);
    }

    @GetMapping("/chat/rooms")
    @ResponseBody
    public List<ChatRoom> getUserChatRooms(Principal principal) {
        String userId = extractUserId(principal);

        List<ChatRoom> asTraveler = chatRoomRepository.findByTravelerId(userId);
        List<ChatRoom> asHost = chatRoomRepository.findByHostId(userId);
        asTraveler.addAll(asHost);
        return asTraveler;
    }

    @GetMapping("/chat/history/{roomId}")
    @ResponseBody
    public List<ChatMessage> getChatHistory(@PathVariable String roomId) {
        return chatMessageRepository.findByRoomIdOrderByTimestampAsc(roomId);
    }

    // 4. RICEZIONE E INVIO IN TEMPO REALE (WebSocket + Notifica Automatica)
    @MessageMapping("/chat.sendMessage")
    public void processMessage(@Payload ChatMessage chatMessage, Principal principal) {
        String senderId = "anonymous";
        if (principal != null) {
            senderId = extractUserId(principal);
            chatMessage.setSenderId(senderId);
        }

        System.out.println("DEBUG: Messaggio ricevuto per la stanza " + chatMessage.getRoomId() +
                " da " + senderId + ". Testo: " + chatMessage.getContent());

        // Salva il messaggio nel DB
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

        // Invia il messaggio in tempo reale sulla WebSocket della stanza
        messagingTemplate.convertAndSend("/topic/room/" + chatMessage.getRoomId(), savedMessage);

        // --- 🚀 MAGIA DELLE NOTIFICHE AUTOMATICHE ---
        // Troviamo la chat room per capire chi è l'altro utente (destinatario)
        Optional<ChatRoom> roomOpt = chatRoomRepository.findById(chatMessage.getRoomId());
        if (roomOpt.isPresent()) {
            ChatRoom room = roomOpt.get();
            // Il destinatario è l'altro partecipante (se chi manda è il traveler, riceve l'host, e viceversa)
            String recipientId = senderId.equals(room.getTravelerId()) ? room.getHostId() : room.getTravelerId();

            if (recipientId != null) {
                // Creiamo l'evento notifica
                NotificationEvent notificationEvent = new NotificationEvent(
                        recipientId,
                        "Nuovo messaggio 💬",
                        "Hai ricevuto un nuovo messaggio in chat."
                );

                // Spediamo l'evento a RabbitMQ (che farà scattare il Consumer e arriverà sul telefono del destinatario)
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.NOTIFICATION_QUEUE,
                        notificationEvent
                );
            }
        }
    }
}