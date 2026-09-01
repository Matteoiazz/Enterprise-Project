package com.tripify.communication_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // L'ID progressivo del singolo messaggio può rimanere Long o diventare UUID, ma come chiave primaria numerica va benissimo

    @Column(nullable = false)
    private String roomId; // ID della ChatRoom (ora è una Stringa UUID)

    @Column(nullable = false)
    private String senderId; // Chi invia il messaggio (UUID di Keycloak)

    @Column(nullable = false, length = 1000)
    private String content; // Il testo del messaggio

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "is_read", nullable = false, columnDefinition = "boolean not null default false")
    private Boolean isRead = false;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}