package com.tripify.booking_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "booking", indexes = @Index(name = "idx_booking_user_id", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId; // Questo è il LEADER (chi paga/crea il viaggio)

    // LA NOVITÀ: Lista degli User ID degli amici invitati
    @ElementCollection
    @CollectionTable(name = "booking_participants", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "participant_id")
    @Builder.Default
    private Set<String> participantIds = new HashSet<>();

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private LocalDateTime bookingDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    // Chiave di idempotenza opzionale (header Idempotency-Key su /checkout):
    // un retry con la stessa chiave restituisce la Booking già creata invece
    // di duplicarla. Nullable, resta valida anche senza header.
    @Column(unique = true)
    private String idempotencyKey;

    // Lock ottimistico: senza questo, due richieste concorrenti sulla stessa
    // Booking (es. due cancelBooking() in corsa) leggono lo stesso stato
    // "vecchio" e l'ultima a salvare sovrascrive l'altra in silenzio - nel
    // peggiore dei casi un doppio rimborso. Con @Version la seconda trova la
    // riga già cambiata e fallisce (mappato a 409 in GlobalExceptionHandler).
    @Version
    private Long version;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BookingLine> lines = new java.util.ArrayList<>();

    // Scritto una sola volta da Hibernate all'INSERT. Non toccarlo mai a mano.
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Riscritto automaticamente da Hibernate ad ogni UPDATE della riga.
    // Non serve più chiamare setUpdatedAt(...) manualmente da nessuna parte.
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}