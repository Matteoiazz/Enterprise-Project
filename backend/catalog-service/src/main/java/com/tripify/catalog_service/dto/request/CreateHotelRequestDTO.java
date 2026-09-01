package com.tripify.catalog_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Campi accettati in creazione: niente id/hostId/isActive/isUserGenerated/rating,
 * che sono decisi dal server (vedi CatalogController.createHotel).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateHotelRequestDTO {

    @NotBlank(message = "il titolo è obbligatorio")
    private String title;

    private String description;

    @NotNull(message = "il prezzo è obbligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "il prezzo deve essere maggiore di zero")
    private BigDecimal price;

    @NotBlank(message = "la valuta è obbligatoria")
    @Size(min = 3, max = 3, message = "la valuta deve essere un codice ISO a 3 lettere (es. EUR)")
    private String currency;

    private String category;

    @NotNull(message = "la latitudine è obbligatoria")
    @DecimalMin(value = "-90.0", message = "latitudine non valida")
    @DecimalMax(value = "90.0", message = "latitudine non valida")
    private Double locationLat;

    @NotNull(message = "la longitudine è obbligatoria")
    @DecimalMin(value = "-180.0", message = "longitudine non valida")
    @DecimalMax(value = "180.0", message = "longitudine non valida")
    private Double locationLng;

    @NotBlank(message = "l'indirizzo è obbligatorio")
    private String address;

    @NotBlank(message = "la città è obbligatoria")
    private String city;

    private List<String> amenities = new ArrayList<>();

    @NotEmpty(message = "serve almeno una tipologia di camera")
    @Valid
    private List<RoomTypeRequestDTO> roomTypes;
}
