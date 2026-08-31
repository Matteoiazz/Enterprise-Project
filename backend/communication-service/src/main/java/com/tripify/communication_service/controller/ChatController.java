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
    public ChatRoom getOrCreateChatRoom(
            @RequestParam String hostId,
            @RequestParam(required = false) String title, // Nuovo parametro
            Principal principal) {

        String travelerId = extractUserId(principal);

        Optional<ChatRoom> existingRoom = chatRoomRepository.findByTravelerIdAndHostId(travelerId, hostId);
        if (existingRoom.isPresent()) {
            return existingRoom.get();
        }

        ChatRoom newRoom = new ChatRoom();
        newRoom.setTravelerId(travelerId);
        newRoom.setHostId(hostId);
        newRoom.setTitle(title); // Salviamo il titolo
        return chatRoomRepository.save(newRoom);
    }

    @GetMapping("/chat/rooms")
    @ResponseBody
    public List<com.tripify.communication_service.dto.ChatRoomDto> getUserChatRooms(Principal principal) {
        String userId = extractUserId(principal);
        System.out.println("DEBUG CHAT ROOMS: L'utente che richiede le chat è -> " + userId);

        List<ChatRoom> asTraveler = chatRoomRepository.findByTravelerId(userId);
        List<ChatRoom> asHost = chatRoomRepository.findByHostId(userId);

        List<ChatRoom> allRooms = new java.util.ArrayList<>(asTraveler);
        allRooms.addAll(asHost);

        return allRooms.stream().map(room -> {
            com.tripify.communication_service.dto.ChatRoomDto dto = new com.tripify.communication_service.dto.ChatRoomDto();
            dto.setId(room.getId());
            dto.setTravelerId(room.getTravelerId());
            dto.setHostId(room.getHostId());
            dto.setTitle(room.getTitle());

            // Calcola dinamicamente quanti messaggi non letti hai in questa chat
            int unread = chatMessageRepository.countByRoomIdAndSenderIdNotAndIsReadFalse(room.getId(), userId);
            dto.setUnreadCount(unread);

            return dto;
        }).toList();
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

        chatMessage.setIsRead(false);

        System.out.println("DEBUG: Messaggio ricevuto per la stanza " + chatMessage.getRoomId() +
                " da " + senderId + ". Testo: " + chatMessage.getContent());

        // Salva il messaggio nel DB
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

        // Invia il messaggio in tempo reale sulla WebSocket della stanza
        messagingTemplate.convertAndSend("/topic/room/" + chatMessage.getRoomId(), savedMessage);

        // Notifica RabbitMQ per l'altro utente
        Optional<ChatRoom> roomOpt = chatRoomRepository.findById(chatMessage.getRoomId());
        if (roomOpt.isPresent()) {
            ChatRoom room = roomOpt.get();
            String recipientId = senderId.equals(room.getTravelerId()) ? room.getHostId() : room.getTravelerId();

            if (recipientId != null) {
                NotificationEvent notificationEvent = new NotificationEvent(
                        recipientId,
                        "Nuovo messaggio 💬",
                        "Hai ricevuto un nuovo messaggio in chat."
                );

                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.NOTIFICATION_QUEUE,
                        notificationEvent
                );
            }
        }
    }

    @PutMapping("/chat/rooms/{roomId}/read")
    @ResponseBody
    public void markMessagesAsRead(@PathVariable String roomId, Principal principal) {
        String userId = extractUserId(principal);
        chatMessageRepository.markAsReadByRoomAndRecipient(roomId, userId);
        System.out.println("DEBUG: Messaggi segnati come letti per l'utente " + userId + " nella stanza " + roomId);
    }
}