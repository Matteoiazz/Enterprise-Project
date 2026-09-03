package com.tripify.communication_service.controller;

import com.tripify.communication_service.entity.Notification;
import com.tripify.communication_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    private String extractUserId(Principal principal) {
        if (principal == null) {
            return "anonymous";
        }
        if (principal instanceof JwtAuthenticationToken jwtToken) {
            return jwtToken.getToken().getSubject();
        }
        return principal.getName();
    }

    @GetMapping
    public ResponseEntity<List<Notification>> getMyNotifications(Principal principal) {
        String userId = extractUserId(principal);
        List<Notification> notifications = notificationService.getUserNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable Long id, Principal principal) {
        String userId = extractUserId(principal);
        Notification updated = notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(Principal principal) {
        String userId = extractUserId(principal);
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(count);
    }
}