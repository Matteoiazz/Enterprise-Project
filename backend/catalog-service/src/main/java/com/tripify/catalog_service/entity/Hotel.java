package com.tripify.catalog_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hotel_details")
@Data
@EqualsAndHashCode(callSuper = true, exclude = "roomTypes")
@ToString(exclude = "roomTypes")
@NoArgsConstructor
@AllArgsConstructor
public class Hotel extends CatalogItem {

    @NotNull(message = "la latitudine è obbligatoria")
    @DecimalMin(value = "-90.0", message = "latitudine non valida")
    @DecimalMax(value = "90.0", message = "latitudine non valida")
    @Column(name = "location_lat", nullable = false)
    private Double locationLat;

    @NotNull(message = "la longitudine è obbligatoria")
    @DecimalMin(value = "-180.0", message = "longitudine non valida")
    @DecimalMax(value = "180.0", message = "longitudine non valida")
    @Column(name = "location_lng", nullable = false)
    private Double locationLng;

    @NotBlank(message = "l'indirizzo è obbligatorio")
    @Column(name = "address", nullable = false)
    private String address;

    @NotBlank(message = "la città è obbligatoria")
    @Column(name = "city", nullable = false)
    private String city;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hotel_amenities", joinColumns = @JoinColumn(name = "hotel_id"))
    @Column(name = "amenity")
    private List<String> amenities = new ArrayList<>();

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomType> roomTypes = new ArrayList<>();
}