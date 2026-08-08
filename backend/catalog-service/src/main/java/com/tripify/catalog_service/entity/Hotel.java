package com.tripify.catalog_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hotel_details")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Hotel extends CatalogItem {

    @Column(name = "location_lat", nullable = false)
    private Double locationLat;

    @Column(name = "location_lng", nullable = false)
    private Double locationLng;

    @Column(name = "room_type", nullable = false)
    private String roomType;

    @Column(name = "available_rooms", nullable = false)
    private Integer availableRooms;

    @Column(name = "address", nullable = false)
    private String address;

    // Tabella separata "hotel_amenities" con FK verso hotel_details
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hotel_amenities", joinColumns = @JoinColumn(name = "hotel_id"))
    @Column(name = "amenity")
    private List<String> amenities = new ArrayList<>();
}