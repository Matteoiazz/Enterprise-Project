package com.tripify.booking_service.controller;

import com.tripify.booking_service.dto.PaymentRequestDTO;
import com.tripify.booking_service.dto.PaymentResultDTO;
import com.tripify.booking_service.entity.Booking;
import com.tripify.booking_service.exception.PaymentValidationException;
import com.tripify.booking_service.service.BookingService;
import com.tripify.booking_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingService bookingService;

    // userId letto dal claim "sub" del JWT già verificato da Spring Security,
    // non più dall'header X-User-Id (era falsificabile da chiunque avesse un
    // JWT valido, anche per pagare/vedere il carrello di un altro utente).
    @PostMapping("/process")
    public ResponseEntity<PaymentResultDTO> processPayment(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody PaymentRequestDTO request) {

        String userId = jwt.getSubject();

        boolean hasCardNumber = request.cardNumber() != null && !request.cardNumber().isBlank();
        boolean hasPaymentMethodId = request.paymentMethodId() != null && !request.paymentMethodId().isBlank();
        if (!hasCardNumber && !hasPaymentMethodId) {
            throw new PaymentValidationException("Specificare un numero di carta o un metodo di pagamento salvato.");
        }

        boolean approved = paymentService.executePayment(
                userId, request.bookingId(), request.cardNumber(), request.paymentMethodId(), request.amount());

        if (!approved) {
            return ResponseEntity.badRequest().body(
                    new PaymentResultDTO(false, "Transazione fallita. Carta non valida.", request.bookingId(), null));
        }

        // Solo dopo l'approvazione della "banca" transizioniamo davvero la
        // Booking a CONFIRMED: verifica proprietario/importo/stato e conferma
        // gli hold su catalog-service (vedi BookingService.confirmPayment).
        Booking booking = bookingService.confirmPayment(request.bookingId(), userId, request.amount());

        return ResponseEntity.ok(new PaymentResultDTO(
                true, "Pagamento approvato.", booking.getId(), booking.getStatus()));
    }
}
