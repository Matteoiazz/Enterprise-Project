package com.tripify.catalog_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Campi accettati in creazione: niente id/hostId/isActive/isUserGenerated/rating,
 * che sono decisi dal server (vedi CatalogController.createFlight).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFlightRequestDTO {

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

    @NotBlank(message = "l'aeroporto di partenza è obbligatorio")
    @Size(min = 3, max = 3, message = "il codice IATA deve avere 3 lettere")
    private String departureAirport;

    @NotBlank(message = "l'aeroporto di arrivo è obbligatorio")
    @Size(min = 3, max = 3, message = "il codice IATA deve avere 3 lettere")
    private String arrivalAirport;

    @NotBlank(message = "la città di partenza è obbligatoria")
    private String departureCity;

    @NotBlank(message = "la città di arrivo è obbligatoria")
    private String arrivalCity;

    @NotNull(message = "l'orario di partenza è obbligatorio")
    private LocalDateTime departureTime;

    @NotNull(message = "l'orario di arrivo è obbligatorio")
    private LocalDateTime arrivalTime;

    @NotNull(message = "il numero di posti totali è obbligatorio")
    @Min(value = 0, message = "i posti totali non possono essere negativi")
    private Integer totalSeats;

    @NotNull
    @Min(value = 0, message = "il numero di scali non può essere negativo")
    private Integer stops = 0;

    @NotEmpty(message = "serve almeno una classe tariffaria")
    @Valid
    private List<FareClassRequestDTO> fareClasses;

    @AssertTrue(message = "l'orario di arrivo deve essere successivo a quello di partenza")
    public boolean isArrivalAfterDeparture() {
        return departureTime == null || arrivalTime == null || arrivalTime.isAfter(departureTime);
    }
}
