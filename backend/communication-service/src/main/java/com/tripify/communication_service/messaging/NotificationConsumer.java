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

        // Salviamo la notifica nel database tramite il service creato prima
        notificationService.createNotification(
                event.getUserId() != null ? event.getUserId().toString() : "anonymous",
                event.getTitle(),
                event.getMessage()
        );

        System.out.println("Nuova notifica salvata per l'utente " + event.getUserId() + ": " + event.getTitle());
    }
}