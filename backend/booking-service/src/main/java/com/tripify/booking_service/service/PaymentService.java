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
    // nemmeno lì). L'appartenenza del metodo salvato all'utente che ha
    // chiamato è verificata qui (serve UserAuthClient, non è compito del
    // gateway di pagamento); l'addebito vero e proprio (anche solo simulato)
    // è delegato a PaymentGateway, così PaymentService non deve sapere come è
    // fatto un numero di carta valido o come si registra un rimborso.
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

    // Rimborso in caso di cancellazione: registra almeno un riferimento alla
    // transazione (anche se solo simulata, vedi MockPaymentGateway), invece
    // di limitarsi a un log senza alcuna traccia di *cosa* è stato rimborsato.
    public void refund(Long bookingId, BigDecimal amount) {
        String reference = paymentGateway.refund(bookingId, amount);
        log.info("Rimborso registrato per la prenotazione {} (riferimento {})", bookingId, reference);
    }
}