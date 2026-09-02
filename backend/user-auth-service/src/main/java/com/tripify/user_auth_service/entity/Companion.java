package com.tripify.user_auth_service.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "companions", indexes = @Index(name = "idx_companions_user", columnList = "user_id"))
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Companion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    // Molti compagni sono salvati nella rubrica di UN utente
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}