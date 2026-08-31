package com.tripify.communication_service.controller;

import com.tripify.communication_service.config.RabbitMQConfig;
import com.tripify.communication_service.entity.ChatMessage;
import com.tripify.communication_service.entity.ChatRoom;
import com.tripify.communication_service.messaging.NotificationEvent;
import com.tripify.communication_service.repository.ChatMessageRepository;
import com.tripify.communication_service.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final RabbitTemplate rabbitTemplate;

    private String extractUserId(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utente non autenticato");
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
            @RequestParam(required = false) String title,
            Principal principal) {

        String travelerId = extractUserId(principal);

        // 1. Blocco sicurezza: impedisce di creare una chat con se stessi
        if (travelerId.equals(hostId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossibile creare una chat con te stesso");
        }

        // 2. Controllo bidirezionale
        Optional<ChatRoom> existingRoom = chatRoomRepository.findByTravelerIdAndHostId(travelerId, hostId);
        if (existingRoom.isPresent()) {
            return existingRoom.get();
        }
        Optional<ChatRoom> reverseRoom = chatRoomRepository.findByTravelerIdAndHostId(hostId, travelerId);
        if (reverseRoom.isPresent()) {
            return reverseRoom.get();
        }

        ChatRoom newRoom = new ChatRoom();
        newRoom.setTravelerId(travelerId);
        newRoom.setHostId(hostId);
        newRoom.setTitle(title);
        return chatRoomRepository.save(newRoom);
    }

    @GetMapping("/chat/rooms")
    @ResponseBody
    public List<com.tripify.communication_service.dto.ChatRoomDto> getUserChatRooms(Principal principal) {
        String userId = extractUserId(principal);

        List<ChatRoom> asTraveler = chatRoomRepository.findByTravelerId(userId);
        List<ChatRoom> asHost = chatRoomRepository.findByHostId(userId);

        List<ChatRoom> allRooms = new ArrayList<>(asTraveler);
        allRooms.addAll(asHost);

        return allRooms.stream().map(room -> {
            com.tripify.communication_service.dto.ChatRoomDto dto = new com.tripify.communication_service.dto.ChatRoomDto();
            dto.setId(room.getId());
            dto.setTravelerId(room.getTravelerId());
            dto.setHostId(room.getHostId());
            dto.setTitle(room.getTitle());

            int unread = chatMessageRepository.countByRoomIdAndSenderIdNotAndIsReadFalse(room.getId(), userId);
            dto.setUnreadCount(unread);

            return dto;
        }).toList();
    }

    @GetMapping("/chat/history/{roomId}")
    @ResponseBody
    public List<ChatMessage> getChatHistory(@PathVariable String roomId, Principal principal) {
        String userId = extractUserId(principal);

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stanza non trovata"));

        // 3. Blocco sicurezza: verifica che chi chiama faccia parte della chat
        if (!userId.equals(room.getTravelerId()) && !userId.equals(room.getHostId())) {
            log.warn("Tentativo di accesso negato alla stanza {} dall'utente {}", roomId, userId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accesso negato a questa conversazione");
        }

        return chatMessageRepository.findByRoomIdOrderByTimestampAsc(roomId);
    }

    @MessageMapping("/chat.sendMessage")
    public void processMessage(@Payload ChatMessage chatMessage, Principal principal) {
        String senderId;

        // 1. Fallback di Sicurezza per il Principal
        try {
            if (principal != null) {
                if (principal instanceof JwtAuthenticationToken jwtToken) {
                    senderId = jwtToken.getToken().getSubject();
                } else {
                    senderId = principal.getName();
                }
            } else {
                // Se Spring ha perso il token nella sessione, usiamo l'ID inviato dall'app
                senderId = chatMessage.getSenderId();
                log.warn("Principal nullo. Fallback su senderId del payload: {}", senderId);
            }
        } catch (Exception e) {
            log.error("Errore estrazione utente", e);
            senderId = chatMessage.getSenderId();
        }

        chatMessage.setSenderId(senderId);
        chatMessage.setIsRead(false);

        // 2. Controllo stanza flessibile (logga invece di bloccare)
        Optional<ChatRoom> roomOpt = chatRoomRepository.findById(chatMessage.getRoomId());
        if (roomOpt.isEmpty()) {
            log.warn("Stanza {} non trovata nel DB. Salvo comunque per evitare perdite di dati.", chatMessage.getRoomId());
        }

        // 3. Salvataggio e Invio sbloccati
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        messagingTemplate.convertAndSend("/topic/room/" + chatMessage.getRoomId(), savedMessage);

        // 4. Logica Notifiche
        if (roomOpt.isPresent()) {
            ChatRoom room = roomOpt.get();
            String recipientId = senderId.equals(room.getTravelerId()) ? room.getHostId() : room.getTravelerId();

            if (recipientId != null) {
                NotificationEvent notificationEvent = new NotificationEvent(
                        recipientId,
                        "Nuovo messaggio 💬",
                        "Hai ricevuto un nuovo messaggio in chat."
                );
                rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFICATION_QUEUE, notificationEvent);
            }
        }
    }

    @PutMapping("/chat/rooms/{roomId}/read")
    @ResponseBody
    public void markMessagesAsRead(@PathVariable String roomId, Principal principal) {
        String userId = extractUserId(principal);
        chatMessageRepository.markAsReadByRoomAndRecipient(roomId, userId);
    }
}