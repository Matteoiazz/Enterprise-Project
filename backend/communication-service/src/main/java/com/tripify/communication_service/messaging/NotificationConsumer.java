package com.tripify.communication_service.messaging;

import com.tripify.communication_service.service.NotificationService;
import com.tripify.communication_service.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;

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

        System.out.println("Nuova notifica salvata per l'utente " + userId + ": " + event.getTitle());
    }
}