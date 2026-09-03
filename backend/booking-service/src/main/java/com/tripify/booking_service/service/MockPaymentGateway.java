package com.tripify.booking_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

// ATTENZIONE: nessun addebito reale, nessun collegamento a un PSP. Approva o
// rifiuta solo su controlli sintattici (formato/checksum carta, importo
// positivo). Da sostituire prima di qualunque uso oltre demo/sviluppo.
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

    // Solo cifre, lunghezza 13-19 e Luhn valido: prima bastava una stringa
    // qualsiasi di almeno 12 caratteri, anche "aaaaaaaaaaaa".
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
