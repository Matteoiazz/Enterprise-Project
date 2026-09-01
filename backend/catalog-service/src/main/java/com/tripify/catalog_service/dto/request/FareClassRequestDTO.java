package com.tripify.catalog_service.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FareClassRequestDTO {

    @NotBlank(message = "il nome della classe è obbligatorio")
    private String name;

    @NotNull(message = "il prezzo è obbligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "il prezzo deve essere maggiore di zero")
    private BigDecimal price;

    @NotNull(message = "il numero di posti di questa classe è obbligatorio")
    @Min(value = 1, message = "deve esserci almeno un posto in questa classe")
    private Integer totalSeats;
}
