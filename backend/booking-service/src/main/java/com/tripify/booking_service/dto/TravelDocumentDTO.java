package com.tripify.booking_service.dto;

import java.time.LocalDate;
import java.util.UUID;

public record TravelDocumentDTO(
        UUID id,
        String documentType,
        String documentNumber,
        LocalDate expirationDate,
        String issuingCountry
) {}