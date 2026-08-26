package com.tripify.booking_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "booking_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false)
    private Long catalogItemId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // Quantità acquistata per questa riga (camere/posti/partecipanti), ereditata
    // dal CartItem al checkout: serve per sapere quanti Passenger si possono
    // associare al massimo a questa riga (vedi BookingService.addPassenger).
    // Volutamente nullable a livello di colonna (nonostante il codice la
    // valorizzi sempre): con spring.jpa.hibernate.ddl-auto=update, aggiungere
    // una NOT NULL su una tabella già popolata farebbe fallire l'ALTER TABLE.
    private Integer quantity;

    // Stessi campi del CartItem da cui questa riga deriva: servono a portare
    // avanti dopo il checkout quale hold di catalog-service proteggeva
    // l'acquisto, per poterlo confermare (pagamento riuscito) o rilasciare
    // (annullamento) senza doverlo ricalcolare.
    private Long roomTypeId;
    private Long fareClassId;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private String holdId;

    @OneToMany(mappedBy = "bookingLine", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Passenger> passengers = new java.util.ArrayList<>();
}