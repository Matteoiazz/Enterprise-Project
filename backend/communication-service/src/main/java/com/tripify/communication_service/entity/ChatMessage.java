package com.tripify.communication_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Data // Se usi Lombok, ti genera in automatico Getter, Setter e Costruttori
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long senderId; // Chi invia il messaggio

    @Column(nullable = false)
    private Long receiverId; // Chi riceve il messaggio

    @Column(nullable = false, length = 1000)
    private String content; // Il testo del messaggio

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}