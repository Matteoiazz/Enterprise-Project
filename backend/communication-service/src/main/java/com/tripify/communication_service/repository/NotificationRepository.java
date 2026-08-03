package com.tripify.communication_service.repository;

import com.tripify.communication_service.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Spring Data JPA capirà automaticamente di ordinare per data decrescente
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
}