package com.tripify.catalog_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;


@Entity
@Table(name = "fare_classes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FareClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @NotBlank(message = "il nome della classe è obbligatorio")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "il prezzo è obbligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "il prezzo deve essere maggiore di zero")
    @Column(nullable = false)
    private BigDecimal price;

    @NotNull(message = "il numero di posti di questa classe è obbligatorio")
    @Min(value = 1, message = "deve esserci almeno un posto in questa classe")
    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;
}
