package com.tripify.communication_service.messaging;

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

    // Questo metodo scatta in automatico quando arriva un messaggio
    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void consumeNotification(NotificationEvent event) {

        // Estraiamo e puliamo l'ID utente assegnandolo alla variabile
        String userId = event.getUserId() != null ? event.getUserId() : "anonymous";

        // Salviamo la notifica nel database usando la variabile pulita
        notificationService.createNotification(
                userId,
                event.getTitle(),
                event.getMessage()
        );

        log.info("Nuova notifica salvata per l'utente {}: {}", userId, event.getTitle());
        // 2. MAGIA DEL TEMPO REALE: Invio sul canale WebSocket personale dell'utente
        messagingTemplate.convertAndSend("/topic/notifications/" + userId, event);
    }
}