package com.tripify.booking_service.service;

import com.tripify.booking_service.client.UserAuthClient;
import com.tripify.booking_service.dto.PaymentMethodDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * PaymentService non ha bisogno di un DB (nessun @DataJpaTest): l'unica dipendenza
 * è il Feign client verso user-auth-service, quindi basta un mock puro Mockito.
 * Copre le due modalità di pagamento: carta nuova (validazione locale sulla
 * lunghezza) e metodo salvato (validazione tramite corrispondenza dell'id con
 * quelli restituiti da user-auth-service per l'utente corrente).
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private UserAuthClient userAuthClient;

    private PaymentService paymentService;

    private static final String USER_ID = "leader-1";
    private static final Long BOOKING_ID = 1L;
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(100.0);

    // MockPaymentGateway non ha dipendenze proprie: usarne un'istanza vera
    // (non un mock) tiene questi test end-to-end sulla validazione reale del
    // numero di carta (Luhn), non solo sull'orchestrazione di PaymentService.
    private final PaymentGateway paymentGateway = new MockPaymentGateway();

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(userAuthClient, paymentGateway);
    }

    @Test
    void approvaUnaCartaNuovaConNumeroPlausibile() {
        // 4111 1111 1111 1111: numero di test Visa standard, valido per Luhn.
        boolean approved = paymentService.executePayment(USER_ID, BOOKING_ID, "4111111111111111", null, AMOUNT);

        assertThat(approved).isTrue();
    }

    @Test
    void rifiutaUnaCartaNuovaTroppoCorta() {
        boolean approved = paymentService.executePayment(USER_ID, BOOKING_ID, "123", null, AMOUNT);

        assertThat(approved).isFalse();
    }

    @Test
    void rifiutaUnaCartaConCifreSufficientiMaChecksumLuhnNonValido() {
        // Stessa lunghezza di un numero di carta reale, ma non passa Luhn:
        // prima sarebbe stata approvata solo perché lunga almeno 12 caratteri.
        boolean approved = paymentService.executePayment(USER_ID, BOOKING_ID, "4111111111111112", null, AMOUNT);

        assertThat(approved).isFalse();
    }

    @Test
    void rifiutaUnaCartaConSoleLettere() {
        boolean approved = paymentService.executePayment(USER_ID, BOOKING_ID, "aaaaaaaaaaaa", null, AMOUNT);

        assertThat(approved).isFalse();
    }

    @Test
    void rifiutaUnImportoNonPositivo() {
        boolean approved = paymentService.executePayment(USER_ID, BOOKING_ID, "4111111111111111", null, BigDecimal.ZERO);

        assertThat(approved).isFalse();
    }

    @Test
    void approvaUnMetodoSalvatoSeAppartieneAllUtente() {
        UUID methodId = UUID.randomUUID();
        when(userAuthClient.getPaymentMethods()).thenReturn(
                List.of(new PaymentMethodDTO(methodId, "Visa", "4242", "12/28")));

        boolean approved = paymentService.executePayment(USER_ID, BOOKING_ID, null, methodId.toString(), AMOUNT);

        assertThat(approved).isTrue();
    }

    @Test
    void rifiutaUnMetodoSalvatoSeNonAppartieneAllUtente() {
        when(userAuthClient.getPaymentMethods()).thenReturn(
                List.of(new PaymentMethodDTO(UUID.randomUUID(), "Visa", "4242", "12/28")));

        boolean approved = paymentService.executePayment(USER_ID, BOOKING_ID, null, UUID.randomUUID().toString(), AMOUNT);

        assertThat(approved).isFalse();
    }

    @Test
    void ilPaymentMethodIdHaPriorataSulCardNumberQuandoEntrambiPresenti() {
        UUID methodId = UUID.randomUUID();
        when(userAuthClient.getPaymentMethods()).thenReturn(
                List.of(new PaymentMethodDTO(methodId, "Visa", "4242", "12/28")));

        // cardNumber deliberatamente non valido: se venisse considerato, il test fallirebbe
        boolean approved = paymentService.executePayment(USER_ID, BOOKING_ID, "1", methodId.toString(), AMOUNT);

        assertThat(approved).isTrue();
    }
}
