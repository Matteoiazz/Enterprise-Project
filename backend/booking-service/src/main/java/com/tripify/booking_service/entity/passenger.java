package com.tripify.booking_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "passengers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "booking_line_id", nullable = false)
    private bookingLine bookingLine;

    // Dati "congelati" al momento dell'acquisto
    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String taxCode;

    // Campi per il Check-in
    private String qrCodeData;

    @Column(nullable = false)
    private boolean checkedIn = false;
}
