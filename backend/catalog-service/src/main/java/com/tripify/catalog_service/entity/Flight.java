package com.tripify.catalog_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "flight_details")
@Data
@EqualsAndHashCode(callSuper = true) // Importante per l'ereditarietà con Lombok
@NoArgsConstructor
@AllArgsConstructor
public class Flight extends CatalogItem {

    @Column(name = "departure_airport", nullable = false, length = 3) // Es: "FCO", "MXP"
    private String departureAirport;

    @Column(name = "arrival_airport", nullable = false, length = 3) // Es: "LHR", "JFK"
    private String arrivalAirport;

    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @Column(name = "arrival_time", nullable = false)
    private LocalDateTime arrivalTime;

    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats;
}
