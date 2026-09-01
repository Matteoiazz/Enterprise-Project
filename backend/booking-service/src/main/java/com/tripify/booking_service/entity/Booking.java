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

    // Chiave di idempotenza opzionale fornita dal client (header Idempotency-Key
    // su POST /checkout): permette di riconoscere un retry dello stesso tentativo
    // di checkout (doppio tap, timeout di rete con richiesta in realtà riuscita)
    // e restituire la Booking già creata invece di crearne una seconda. Nullable
    // perché resta valida anche una richiesta senza header (nessuna protezione
    // aggiuntiva oltre al lock sul carrello, ma comportamento invariato).
    @Column(unique = true)
    private String idempotencyKey;

    // Lock ottimistico: senza questo, due richieste concorrenti sulla stessa
    // Booking (es. due cancelBooking() sulla stessa prenotazione confermata,
    // o un confirmPayment() e un cancelBooking() in corsa) leggono entrambe lo
    // stesso stato "vecchio", passano entrambe i controlli, e l'ultima a
    // salvare sovrascrive silenziosamente l'altra - nel caso peggiore un
    // doppio rimborso quando il rimborso sarà reale. Con @Version, la seconda
    // ad arrivare al salvataggio trova la riga già cambiata e fallisce con
    // ObjectOptimisticLockingFailureException (mappata a 409 in
    // GlobalExceptionHandler) invece di procedere alla cieca.
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