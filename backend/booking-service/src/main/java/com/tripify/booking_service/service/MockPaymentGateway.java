package com.tripify.booking_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

// ATTENZIONE: nessun addebito reale. Nessun collegamento a un vero PSP
// (Stripe/Nexi/...): approva o rifiuta in base a soli controlli sintattici
// (formato/checksum del numero carta, importo positivo), non tocca mai una
// carta o un conto veri, non fa 3-D Secure, non verifica scadenza/CVV (non
// arrivano nemmeno al backend, vedi PaymentRequestDTO). Da sostituire con
// un'implementazione reale prima di qualunque uso oltre la demo/lo sviluppo.
@Component
@Slf4j
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public boolean chargeCard(String cardNumber, BigDecimal amount) {
        if (!isValidAmount(amount)) {
            log.error("Importo non valido per l'addebito: {}", amount);
            return false;
        }
        if (!isValidCardNumber(cardNumber)) {
            log.error("Numero di carta non valido (formato o checksum non corretto)");
            return false;
        }
        log.info("[MOCK] Addebito simulato di {}€ sulla carta ****{}", amount, lastFourDigits(cardNumber));
        return true;
    }

    @Override
    public boolean chargeToken(String paymentMethodId, BigDecimal amount) {
        if (!isValidAmount(amount)) {
            log.error("Importo non valido per l'addebito: {}", amount);
            return false;
        }
        log.info("[MOCK] Addebito simulato di {}€ sul metodo di pagamento salvato {}", amount, paymentMethodId);
        return true;
    }

    @Override
    public String refund(Long bookingId, BigDecimal amount) {
        String reference = "MOCK-REFUND-" + UUID.randomUUID();
        log.info("[MOCK] Rimborso simulato di {}€ per la prenotazione {} (riferimento {})", amount, bookingId, reference);
        return reference;
    }

    private boolean isValidAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    // Solo cifre, lunghezza di una carta reale (13-19, standard ISO/IEC 7812)
    // e checksum di Luhn valido: prima bastava una stringa qualsiasi di
    // almeno 12 caratteri, lettere comprese ("aaaaaaaaaaaa" veniva approvato).
    private boolean isValidCardNumber(String cardNumber) {
        if (cardNumber == null) {
            return false;
        }
        String digitsOnly = cardNumber.replace(" ", "");
        if (digitsOnly.length() < 13 || digitsOnly.length() > 19 || !digitsOnly.chars().allMatch(Character::isDigit)) {
            return false;
        }
        return passesLuhnCheck(digitsOnly);
    }

    private boolean passesLuhnCheck(String digits) {
        int sum = 0;
        boolean doubleDigit = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int digit = digits.charAt(i) - '0';
            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }

    private String lastFourDigits(String cardNumber) {
        String digitsOnly = cardNumber.replace(" ", "");
        return digitsOnly.length() >= 4 ? digitsOnly.substring(digitsOnly.length() - 4) : digitsOnly;
    }
}
