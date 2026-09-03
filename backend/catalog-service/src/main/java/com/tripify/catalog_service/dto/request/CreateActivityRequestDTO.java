package com.tripify.catalog_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Campi accettati in creazione: niente id/hostId/isActive/isUserGenerated/rating,
 * che sono decisi dal server (vedi CatalogController.createActivity).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateActivityRequestDTO {

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

    @NotBlank(message = "il tipo di attività è obbligatorio")
    private String activityType;

    @NotBlank(message = "la durata è obbligatoria")
    private String duration;

    private String meetingPoint;

    @NotBlank(message = "la città è obbligatoria")
    private String city;

    @Min(value = 1, message = "i partecipanti massimi devono essere almeno 1")
    private Integer maxParticipants;

    private boolean guideIncluded;
}
