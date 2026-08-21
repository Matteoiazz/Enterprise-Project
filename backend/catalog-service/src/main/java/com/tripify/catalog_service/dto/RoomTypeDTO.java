package com.tripify.catalog_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypeDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer totalRooms;
    private Integer maxOccupancy;
    private List<String> benefits;
    private List<String> imageUrls;
}
