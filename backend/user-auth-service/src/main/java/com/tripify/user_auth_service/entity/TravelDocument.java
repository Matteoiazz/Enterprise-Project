package com.tripify.user_auth_service.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "travel_documents")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TravelDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String documentType; // Es. "PASSAPORTO" o "CARTA_IDENTITA"

    @Column(nullable = false)
    private String documentNumber;

    @Column(nullable = false)
    private LocalDate expirationDate;

    @Column(nullable = false)
    private String issuingCountry;

    // Molti documenti appartengono a UN utente
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}