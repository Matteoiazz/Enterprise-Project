package com.tripify.booking_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "passengers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_line_id", nullable = false)
    private BookingLine bookingLine;

    // Dati "congelati" al momento dell'acquisto
    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String taxCode;

    // Documento di viaggio, anch'esso congelato al momento della prenotazione:
    // Android lo autocompila leggendo da user-auth-service, ma qui salviamo
    // sempre una copia indipendente, non un riferimento vivo al documento originale.
    // Se l'utente aggiorna/rinnova il documento dopo, questa prenotazione
    // continua a riflettere il documento usato in quel momento.
    @Column(nullable = false)
    private String documentType; // Es. "PASSPORT", "ID_CARD"

    @Column(nullable = false)
    private String documentNumber;

    @Column(nullable = false)
    private LocalDate documentExpirationDate;

    @Column(nullable = false, length = 3)
    private String issuingCountry; // Es. "ITA"

    // Campi per il Check-in
    private String qrCodeData;

    @Column(nullable = false)
    private boolean checkedIn = false;
}