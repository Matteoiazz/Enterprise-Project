package com.tripify.catalog_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "flight_details")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Flight extends CatalogItem {

    @NotBlank(message = "l'aeroporto di partenza è obbligatorio")
    @Size(min = 3, max = 3, message = "il codice IATA deve avere 3 lettere")
    @Column(name = "departure_airport", nullable = false, length = 3)
    private String departureAirport;

    @NotBlank(message = "l'aeroporto di arrivo è obbligatorio")
    @Size(min = 3, max = 3, message = "il codice IATA deve avere 3 lettere")
    @Column(name = "arrival_airport", nullable = false, length = 3)
    private String arrivalAirport;

    @NotBlank(message = "la città di partenza è obbligatoria")
    @Column(name = "departure_city", nullable = false)
    private String departureCity;

    @NotBlank(message = "la città di arrivo è obbligatoria")
    @Column(name = "arrival_city", nullable = false)
    private String arrivalCity;

    @NotNull(message = "l'orario di partenza è obbligatorio")
    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @NotNull(message = "l'orario di arrivo è obbligatorio")
    @Column(name = "arrival_time", nullable = false)
    private LocalDateTime arrivalTime;

    @NotNull(message = "il numero di posti disponibili è obbligatorio")
    @Min(value = 0, message = "i posti disponibili non possono essere negativi")
    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats;

    @NotNull
    @Min(value = 0, message = "il numero di scali non può essere negativo")
    @Column(name = "stops", nullable = false)
    private Integer stops = 0;
}