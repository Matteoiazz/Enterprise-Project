package com.tripify.booking_service.service;

import java.math.BigDecimal;

// Isola il fornitore di pagamento da PaymentService (oggi solo
// MockPaymentGateway, nessun addebito reale): qui si innesta un PSP vero
// domani senza toccare BookingService/PaymentController.
public interface PaymentGateway {

    boolean chargeCard(String cardNumber, BigDecimal amount);

    boolean chargeToken(String paymentMethodId, BigDecimal amount);

    // Riferimento al rimborso, per tracciare quale transazione è stata registrata.
    String refund(Long bookingId, BigDecimal amount);
}
