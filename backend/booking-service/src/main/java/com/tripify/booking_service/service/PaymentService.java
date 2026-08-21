package com.tripify.booking_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j // Ci regala i log in console utili per il debug
public class PaymentService {

    // Simula l'addebito sulla carta di credito
    public boolean executePayment(String userId, Long bookingId, String cardNumber, BigDecimal amount) {
        log.info("Avvio pagamento per l'utente {} sul viaggio {}. Importo: {}€", userId, bookingId, amount);

        // Simulazione base: se la carta non è vuota e ha un numero plausibile, il pagamento passa
        if (cardNumber != null && !cardNumber.isBlank() && cardNumber.length() >= 12) {
            log.info("Pagamento APPROVATO dalla banca per la prenotazione {}", bookingId);
            return true;
        }

        log.error("Pagamento RIFIUTATO per la prenotazione {}", bookingId);
        return false;
    }

    // Simula il rimborso in caso di cancellazione
    public void refund(Long bookingId, BigDecimal amount) {
        log.info("Avvio pratica di rimborso per la prenotazione {}. Accredito di {}€ in corso...", bookingId, amount);
    }
}