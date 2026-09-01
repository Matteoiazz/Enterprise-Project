package com.tripify.booking_service.service;

import java.math.BigDecimal;

// Astrae il fornitore di pagamento dietro cui gira PaymentService: oggi
// l'unica implementazione è MockPaymentGateway (nessun addebito reale). Averla
// isolata dietro un'interfaccia rende esplicito che PaymentService non È un
// gateway di pagamento vero, ed è il punto in cui inserire domani un PSP
// reale (Stripe, Nexi, ecc.) senza toccare BookingService/PaymentController,
// che continuerebbero a vedere solo un boolean approvato/rifiutato.
public interface PaymentGateway {

    boolean chargeCard(String cardNumber, BigDecimal amount);

    boolean chargeToken(String paymentMethodId, BigDecimal amount);

    // Ritorna un riferimento alla transazione di rimborso (anche solo
    // simulata): serve perché il chiamante possa almeno loggare/tracciare
    // *quale* rimborso è stato registrato, invece di un generico "fatto".
    String refund(Long bookingId, BigDecimal amount);
}
