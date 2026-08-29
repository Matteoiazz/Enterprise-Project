package com.tripify.communication_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_rooms", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"travelerId", "hostId"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {

    @Id
    private String id; // L'ID della stanza diventa un UUID in formato String

    @Column(nullable = false)
    private String travelerId; // L'ID del viaggiatore (UUID di Keycloak)

    @Column(nullable = false)
    private String hostId; // L'ID dell'organizzatore (UUID di Keycloak)

    // NUOVO CAMPO
    private String title;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString(); // Genera un UUID casuale prima di salvare se non è presente
        }
        this.createdAt = LocalDateTime.now();
    }
}