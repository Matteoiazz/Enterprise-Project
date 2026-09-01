package com.tripify.communication_service.messaging;

import com.tripify.communication_service.dto.NotificationDto;
import com.tripify.communication_service.entity.Notification;
import com.tripify.communication_service.service.NotificationService;
import com.tripify.communication_service.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void consumeNotification(NotificationEvent event) {
        try {
            if (event == null || event.getUserId() == null) {
                log.warn("Ricevuto evento di notifica nullo o privo di userId, scartato.");
                return;
            }

            String userId = event.getUserId();
            String title = event.getTitle() != null ? event.getTitle() : "Notifica";
            String message = event.getMessage();

            // Validazione di sicurezza sulla lunghezza del messaggio (evita crash su DB se > 500 char)
            if (message != null && message.length() > 500) {
                message = message.substring(0, 497) + "...";
            }


            Notification savedNotification = notificationService.createNotification(
                    userId,
                    event.getTitle(),
                    event.getMessage()
            );

            NotificationDto dto = NotificationDto.builder()
                    .id(savedNotification.getId())
                    .userId(savedNotification.getUserId())
                    .title(savedNotification.getTitle())
                    .message(savedNotification.getMessage())
                    .isRead(savedNotification.isRead())
                    .createdAt(savedNotification.getCreatedAt())
                    .build();

            log.info("Notifica salvata e processata con successo per l'utente {}", userId);

            // 3. Invio sul WebSocket del DTO completo (Risolve M2)
            messagingTemplate.convertAndSend("/topic/notifications/" + userId, dto);

        } catch (Exception e) {
            log.error("Errore critico durante l'elaborazione del messaggio RabbitMQ nella NotificationConsumer", e);
            // Rilanciare l'eccezione permette a Spring AMQP di gestire il fallback o la DLQ se configurata
            throw e;
        }
    }
}