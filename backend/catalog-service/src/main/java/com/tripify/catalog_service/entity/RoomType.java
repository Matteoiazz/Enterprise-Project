package com.tripify.catalog_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "room_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @NotBlank(message = "il nome della tipologia è obbligatorio")
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "il prezzo è obbligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "il prezzo deve essere maggiore di zero")
    @Column(nullable = false)
    private BigDecimal price;

    @NotNull(message = "il numero di camere di questo tipo è obbligatorio")
    @Min(value = 1, message = "deve esserci almeno una camera di questo tipo")
    @Column(name = "total_rooms", nullable = false)
    private Integer totalRooms;

    @Min(value = 1, message = "l'occupazione massima deve essere almeno 1")
    @Column(name = "max_occupancy")
    private Integer maxOccupancy;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "room_type_benefits", joinColumns = @JoinColumn(name = "room_type_id"))
    @Column(name = "benefit")
    @Builder.Default
    private List<String> benefits = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "room_type_images", joinColumns = @JoinColumn(name = "room_type_id"))
    @Column(name = "image_url", length = 500)
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();
}
