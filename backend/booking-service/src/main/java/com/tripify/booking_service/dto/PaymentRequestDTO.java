package com.tripify.booking_service.dto;

import java.math.BigDecimal;

// Record: immutabile, niente boilerplate di getter/setter per un DTO che non deve mai cambiare stato.
// Esattamente uno tra cardNumber (nuova carta inserita a mano) e paymentMethodId
// (un metodo già salvato in user-auth-service) deve essere valorizzato: vedi
// PaymentController, che rifiuta la richiesta se sono entrambi nulli.
public record PaymentRequestDTO(
        Long bookingId,
        String cardNumber,
        String paymentMethodId,
        BigDecimal amount
) {}