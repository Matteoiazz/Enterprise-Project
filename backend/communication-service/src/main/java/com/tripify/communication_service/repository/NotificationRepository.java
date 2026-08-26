package com.tripify.communication_service.repository;

import com.tripify.communication_service.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Spring Data JPA capirà automaticamente di ordinare per data decrescente
    // Trova tutte le notifiche di un determinato utente, ordinate da quella più recente
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

    // Conta quante notifiche non lette ha un utente
    long countByUserIdAndIsReadFalse(String userId);
}