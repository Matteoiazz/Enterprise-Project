package com.tripify.booking_service.dto;

import java.time.LocalDate;

// Immutabile: Android manda questi dati già risolti (autocompilati o inseriti
// a mano), il booking-service li congela così come sono nel Passenger.
public record PassengerRequestDTO(
        String firstName,
        String lastName,
        String taxCode,
        String documentType,
        String documentNumber,
        LocalDate documentExpirationDate,
        String issuingCountry
) {}