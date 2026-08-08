package com.tripify.catalog_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogItemDTO {
    // Campi comuni a tutti
    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private String currency;
    private String itemType;
    private String category;
    private Integer rating;
    private List<String> imageUrls;

    // --- CAMPI SPECIFICI DEL VOLO (Flight) ---
    private String departureAirport;
    private String arrivalAirport;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private Integer availableSeats;
    private Integer stops;

    // --- CAMPI SPECIFICI DELL'HOTEL (Hotel) ---
    private String roomType;
    private Integer availableRooms;
    private Double locationLat;
    private Double locationLng;
    private String address;
    private List<String> amenities;

    // --- CAMPI SPECIFICI DELL'ATTIVITÀ (Activity) ---
    private String activityType;
    private String duration;
    private String meetingPoint;
    private Integer maxParticipants;
    private Boolean guideIncluded;
}