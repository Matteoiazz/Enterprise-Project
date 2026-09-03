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
    private final PaymentGateway paymentGateway;

    // Due modalità: una carta nuova inserita a mano (cardNumber) oppure un
    // metodo già salvato su user-auth-service (paymentMethodId) - in quel caso
    // non arriva mai un numero di carta reale (non viene salvato per intero
    // nemmeno lì). L'appartenenza del metodo all'utente si verifica qui;
    // l'addebito vero e proprio è delegato a PaymentGateway.
    public boolean executePayment(String userId, Long bookingId, String cardNumber, String paymentMethodId, BigDecimal amount) {
        log.info("Avvio pagamento per l'utente {} sul viaggio {}. Importo: {}€", userId, bookingId, amount);

        if (paymentMethodId != null && !paymentMethodId.isBlank()) {
            boolean isOwnMethod = userAuthClient.getPaymentMethods().stream()
                    .anyMatch(method -> method.id().toString().equals(paymentMethodId));

            if (!isOwnMethod) {
                log.error("Metodo di pagamento {} non trovato tra quelli salvati dell'utente {}", paymentMethodId, userId);
                return false;
            }

            boolean approved = paymentGateway.chargeToken(paymentMethodId, amount);
            if (approved) {
                log.info("Pagamento APPROVATO con metodo salvato {} per la prenotazione {}", paymentMethodId, bookingId);
            } else {
                log.error("Pagamento RIFIUTATO per la prenotazione {}", bookingId);
            }
            return approved;
        }

        boolean approved = paymentGateway.chargeCard(cardNumber, amount);
        if (approved) {
            log.info("Pagamento APPROVATO dalla banca per la prenotazione {}", bookingId);
        } else {
            log.error("Pagamento RIFIUTATO per la prenotazione {}", bookingId);
        }
        return approved;
    }

    // Registra almeno un riferimento alla transazione, invece di un log
    // senza traccia di cosa è stato rimborsato.
    public void refund(Long bookingId, BigDecimal amount) {
        String reference = paymentGateway.refund(bookingId, amount);
        log.info("Rimborso registrato per la prenotazione {} (riferimento {})", bookingId, reference);
    }
}