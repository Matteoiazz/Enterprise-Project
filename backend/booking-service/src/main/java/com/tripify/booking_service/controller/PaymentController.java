package com.tripify.booking_service.controller;

import com.tripify.booking_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService; // SBLOCCATO

    @PostMapping("/process")
    public ResponseEntity<?> processPayment(
            @RequestParam String userId,
            @RequestParam Long bookingId,
            @RequestParam String cardNumber,
            @RequestParam Double amount) {

        boolean success = paymentService.executePayment(userId, bookingId, cardNumber, amount);
        if (success) {
            return ResponseEntity.ok("Pagamento di " + amount + "€ approvato per la prenotazione " + bookingId);
        } else {
            return ResponseEntity.badRequest().body("Transazione fallita. Carta non valida.");
        }
    }
}