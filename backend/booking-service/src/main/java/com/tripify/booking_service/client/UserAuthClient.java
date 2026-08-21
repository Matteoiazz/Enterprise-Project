package com.tripify.booking_service.client;

import com.tripify.booking_service.dto.PaymentMethodDTO;
import com.tripify.booking_service.dto.TravelDocumentDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

// Path verificati su ProfileController (user-auth-service) reale.
// Non passiamo userId: user-auth-service determina l'utente dal JWT che
// gli inoltriamo (vedi FeignClientConfig) leggendo il claim "email" al suo
// interno - diverso da come noi identifichiamo l'utente nel booking-service
// (claim "sub"). Non è un problema per questa chiamata (user-auth-service
// risolve comunque il chiamante corretto per conto suo), ma è un'inconsistenza
// architetturale nel progetto che vale la pena segnalare al gruppo.
@FeignClient(name = "user-auth-service", url = "${user-auth-service.url}")
public interface UserAuthClient {

    @GetMapping("/api/v1/profile/payments")
    List<PaymentMethodDTO> getPaymentMethods();

    @GetMapping("/api/v1/profile/documents")
    List<TravelDocumentDTO> getTravelDocuments();
}