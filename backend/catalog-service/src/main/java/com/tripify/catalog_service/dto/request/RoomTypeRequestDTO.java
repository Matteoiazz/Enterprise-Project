package com.tripify.catalog_service.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypeRequestDTO {

    @NotBlank(message = "il nome della tipologia è obbligatorio")
    private String name;

    private String description;

    @NotNull(message = "il prezzo è obbligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "il prezzo deve essere maggiore di zero")
    private BigDecimal price;

    @NotNull(message = "il numero di camere di questo tipo è obbligatorio")
    @Min(value = 1, message = "deve esserci almeno una camera di questo tipo")
    private Integer totalRooms;

    @Min(value = 1, message = "l'occupazione massima deve essere almeno 1")
    private Integer maxOccupancy;

    private List<String> benefits = new ArrayList<>();

    private List<String> imageUrls = new ArrayList<>();
}
