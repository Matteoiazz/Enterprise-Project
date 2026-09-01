package com.tripify.booking_service.controller;

import com.tripify.booking_service.client.UserAuthClient;
import com.tripify.booking_service.dto.PaymentMethodDTO;
import com.tripify.booking_service.dto.TravelDocumentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Nota: qui non serve leggere manualmente il JWT (niente @AuthenticationPrincipal),
// perché non usiamo l'userId per nessuna query nostra: ci limitiamo a inoltrare
// la richiesta a user-auth-service, che si occupa lui di determinare l'utente
// dal token propagato da FeignClientConfig.
//
// Path sotto /api/v1/bookings (non /api/v1/checkout, come prima): il gateway
// instrada verso booking-service solo /api/v1/cart/**, /api/v1/bookings/** e
// /api/v1/payments/** (vedi api-gateway/application.yml) - con /api/v1/checkout
// questi due endpoint erano irraggiungibili da chiunque passasse dal gateway.
@RestController
@RequestMapping("/api/v1/bookings/profile")
@RequiredArgsConstructor
public class ProfileProxyController {

    private final UserAuthClient userAuthClient;

    @GetMapping("/payment-methods")
    public ResponseEntity<List<PaymentMethodDTO>> getSavedPaymentMethods() {
        return ResponseEntity.ok(userAuthClient.getPaymentMethods());
    }

    @GetMapping("/travel-documents")
    public ResponseEntity<List<TravelDocumentDTO>> getSavedTravelDocuments() {
        return ResponseEntity.ok(userAuthClient.getTravelDocuments());
    }
}