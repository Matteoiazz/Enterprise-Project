package com.tripify.communication_service.service;

import com.tripify.communication_service.entity.Notification;
import com.tripify.communication_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;

    public Notification createNotification(Long userId, String title, String message) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .build();
        return repository.save(notification);
    }

    public List<Notification> getUserNotifications(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public void markAsRead(Long notificationId) {
        repository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            repository.save(notification);
        });
    }
}