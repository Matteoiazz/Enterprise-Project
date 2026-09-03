package com.tripify.booking_service.dto;

import java.util.UUID;

// Rispecchia solo i campi "sicuri" di PaymentMethod (mai il numero completo,
// che infatti user-auth-service non salva nemmeno per intero).
public record PaymentMethodDTO(
        UUID id,
        String cardProvider,
        String lastFourDigits,
        String expirationMonthYear
) {}