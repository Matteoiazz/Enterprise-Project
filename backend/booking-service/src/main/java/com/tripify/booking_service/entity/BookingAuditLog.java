package com.tripify.booking_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// Tabella append-only: non esiste updatedAt né setter usati dopo la creazione.
// Ogni riga rappresenta un evento immutabile già accaduto.
@Entity
@Table(name = "booking_audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relazione interna alla stessa base dati del booking-service: qui una FK
    // reale ha senso (non è un soft link cross-service come Booking.userId,
    // che invece punta a un'entità di un altro microservizio).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    // Chi ha compiuto l'azione (l'id utente estratto dal JWT nel controller/service)
    @Column(nullable = false)
    private String performedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    // Testo libero con il dettaglio dell'evento, es. "Invitato friendId=42"
    @Column(nullable = false)
    private String details;

    // Scritto una sola volta da Hibernate all'INSERT, mai più toccato dopo.
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}