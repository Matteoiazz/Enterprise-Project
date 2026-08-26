package com.tripify.communication_service.service;

import com.tripify.communication_service.entity.Notification;
import com.tripify.communication_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // 1. Recupera tutte le notifiche di un utente
    public List<Notification> getUserNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // 2. Crea e salva una nuova notifica
    public Notification createNotification(String userId, String title, String message) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .isRead(false)
                .build();
        return notificationRepository.save(notification);
    }

    // 3. Segna una notifica come letta
    public Notification markAsRead(Long notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notifica non trovata"));

        // Controllo di sicurezza: verifichiamo che la notifica appartenga all'utente
        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("Non sei autorizzato a modificare questa notifica");
        }

        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    // 4. Conta le notifiche non lette
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
}