package com.tripify.user_auth_service.controller;

import com.tripify.user_auth_service.dto.request.CompanionDto;
import com.tripify.user_auth_service.dto.request.PaymentMethodDto;
import com.tripify.user_auth_service.dto.request.TravelDocumentDto;
import com.tripify.user_auth_service.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    // --- COMPANIONS ---
    @GetMapping("/companions")
    public ResponseEntity<List<CompanionDto>> getCompanions(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(profileService.getCompanions(jwt.getClaimAsString("email")));
    }

    @PostMapping("/companions")
    public ResponseEntity<CompanionDto> addCompanion(@RequestBody CompanionDto companionDto, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(profileService.addCompanion(jwt.getClaimAsString("email"), companionDto));
    }

    @DeleteMapping("/companions/{id}")
    public ResponseEntity<Void> deleteCompanion(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        profileService.deleteCompanion(jwt.getClaimAsString("email"), id);
        return ResponseEntity.ok().build();
    }

    // --- TRAVEL DOCUMENTS ---
    @GetMapping("/documents")
    public ResponseEntity<List<TravelDocumentDto>> getTravelDocuments(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(profileService.getTravelDocuments(jwt.getClaimAsString("email")));
    }

    @PostMapping("/documents")
    public ResponseEntity<TravelDocumentDto> addTravelDocument(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody TravelDocumentDto dto) {
        return ResponseEntity.ok(profileService.addTravelDocument(jwt.getClaimAsString("email"), dto));
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> deleteTravelDocument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        profileService.deleteTravelDocument(jwt.getClaimAsString("email"), id);
        return ResponseEntity.ok().build();
    }

    // --- PAYMENT METHODS ---
    @GetMapping("/payments")
    public ResponseEntity<List<PaymentMethodDto>> getPayments(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(profileService.getPaymentMethods(jwt.getClaimAsString("email")));
    }

    @PostMapping("/payments")
    public ResponseEntity<PaymentMethodDto> addPayment(@RequestBody PaymentMethodDto paymentMethodDto, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(profileService.addPaymentMethod(jwt.getClaimAsString("email"), paymentMethodDto));
    }

    @DeleteMapping("/payments/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        profileService.deletePaymentMethod(jwt.getClaimAsString("email"), id);
        return ResponseEntity.ok().build();
    }

    // --- PROFILO UTENTE ---
    @GetMapping("/me")
    public ResponseEntity<com.tripify.user_auth_service.dto.response.UserResponse> getMe(@AuthenticationPrincipal Jwt jwt) {
        String firstName = jwt.getClaimAsString("given_name");
        String lastName = jwt.getClaimAsString("family_name");
        String email = jwt.getClaimAsString("email");
        String preferredUsername = jwt.getClaimAsString("preferred_username");

        String displayNome = firstName != null ? firstName : (preferredUsername != null ? preferredUsername : "Utente");
        String displayCognome = lastName != null ? lastName : "";
        String displayEmail = email != null ? email : jwt.getSubject();

        return ResponseEntity.ok(new com.tripify.user_auth_service.dto.response.UserResponse(
                displayNome, displayCognome, displayEmail
        ));
    }
}