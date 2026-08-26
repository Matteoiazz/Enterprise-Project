package com.tripify.communication_service.controller;

import com.tripify.communication_service.entity.Notification;
import com.tripify.communication_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // 1. Ottieni tutte le notifiche dell'utente loggato
    @GetMapping
    public ResponseEntity<List<Notification>> getMyNotifications(Principal principal) {
        // Estrae l'ID utente dalla sessione/token JWT (esattamente come fatto per la chat)
        String userId = principal != null ? principal.getName() : "anonymous";
        List<Notification> notifications = notificationService.getUserNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    // 2. Segna una specifica notifica come letta
    @PatchMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable Long id, Principal principal) {
        String userId = principal != null ? principal.getName() : "anonymous";
        Notification updated = notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(updated);
    }

    // 3. Ottieni il conteggio delle notifiche non lette (utile per la campanella)
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(Principal principal) {
        String userId = principal != null ? principal.getName() : "anonymous";
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(count);
    }
}