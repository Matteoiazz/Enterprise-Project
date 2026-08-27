package com.tripify.booking_service.service;

import com.tripify.booking_service.client.UserAuthClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j // Ci regala i log in console utili per il debug
public class PaymentService {

    private final UserAuthClient userAuthClient;

    // Simula l'addebito sulla carta di credito. Due modalità: una carta nuova
    // inserita a mano (cardNumber) oppure un metodo già salvato su user-auth-service
    // (paymentMethodId) - in quel caso non arriva mai un numero di carta reale
    // (non viene salvato per intero nemmeno lì), quindi il "pagamento" si considera
    // approvato se l'id corrisponde davvero a un metodo dell'utente che ha chiamato.
    public boolean executePayment(String userId, Long bookingId, String cardNumber, String paymentMethodId, BigDecimal amount) {
        log.info("Avvio pagamento per l'utente {} sul viaggio {}. Importo: {}€", userId, bookingId, amount);

        if (paymentMethodId != null && !paymentMethodId.isBlank()) {
            boolean isOwnMethod = userAuthClient.getPaymentMethods().stream()
                    .anyMatch(method -> method.id().toString().equals(paymentMethodId));

            if (isOwnMethod) {
                log.info("Pagamento APPROVATO con metodo salvato {} per la prenotazione {}", paymentMethodId, bookingId);
                return true;
            }

            log.error("Metodo di pagamento {} non trovato tra quelli salvati dell'utente {}", paymentMethodId, userId);
            return false;
        }

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