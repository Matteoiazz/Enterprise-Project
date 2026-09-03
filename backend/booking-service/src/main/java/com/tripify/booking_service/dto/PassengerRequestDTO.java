package com.tripify.booking_service.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// Immutabile: Android manda questi dati già risolti (autocompilati o inseriti
// a mano), il booking-service li congela così come sono nel Passenger.
public record PassengerRequestDTO(
        @NotBlank(message = "il nome è obbligatorio") String firstName,
        @NotBlank(message = "il cognome è obbligatorio") String lastName,
        @NotBlank(message = "il numero di telefono è obbligatorio") String phoneNumber,
        @NotBlank(message = "il codice fiscale è obbligatorio") String taxCode,
        @NotBlank(message = "il tipo di documento è obbligatorio") String documentType,
        @NotBlank(message = "il numero di documento è obbligatorio") String documentNumber,
        @NotNull(message = "la data di scadenza del documento è obbligatoria")
        @Future(message = "il documento è scaduto: la scadenza deve essere successiva a oggi") LocalDate documentExpirationDate,
        @NotBlank(message = "il paese di rilascio è obbligatorio")
        @Size(min = 3, max = 3, message = "il paese di rilascio deve essere un codice ISO a 3 lettere (es. ITA)") String issuingCountry
) {}
