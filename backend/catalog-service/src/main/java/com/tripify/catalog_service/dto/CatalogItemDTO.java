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
    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private String currency;
    private String itemType;
    private String category;
    private Integer rating;
    private List<String> imageUrls;
    // --- CHAT ---
    private String hostId;
    private boolean isUserGenerated;

    // --- VOLO ---
    private String departureAirport;
    private String arrivalAirport;
    private String departureCity;
    private String arrivalCity;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private Integer totalSeats;
    private Integer stops;
    private List<FareClassDTO> fareClasses;

    // --- HOTEL ---
    private Double locationLat;
    private Double locationLng;
    private String address;
    private String city;
    private List<String> amenities;
    private List<RoomTypeDTO> roomTypes;

    // --- ATTIVITÀ ---
    private String activityType;
    private String duration;
    private String meetingPoint;
    private Integer maxParticipants;
    private Boolean guideIncluded;
}