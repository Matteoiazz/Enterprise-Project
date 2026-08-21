package com.tripify.booking_service.controller;

import com.tripify.booking_service.dto.PaymentRequestDTO;
import com.tripify.booking_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // userId ora arriva dall'header impostato dal Gateway, non dal client.
    // Dati sensibili (numero carta, importo) spostati nel body invece che come query param,
    // così non finiscono nei log di accesso del server/proxy.
    @PostMapping("/process")
    public ResponseEntity<?> processPayment(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody PaymentRequestDTO request) {

        boolean success = paymentService.executePayment(
                userId, request.bookingId(), request.cardNumber(), request.amount());

        if (success) {
            return ResponseEntity.ok("Pagamento di " + request.amount() + "€ approvato per la prenotazione " + request.bookingId());
        } else {
            return ResponseEntity.badRequest().body("Transazione fallita. Carta non valida.");
        }
    }
}