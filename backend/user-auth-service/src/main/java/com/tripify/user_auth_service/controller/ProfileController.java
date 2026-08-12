package com.tripify.user_auth_service.controller;

import com.tripify.user_auth_service.dto.request.CompanionDto;
import com.tripify.user_auth_service.dto.request.PaymentMethodDto;
import com.tripify.user_auth_service.dto.request.TravelDocumentDto;
import com.tripify.user_auth_service.entity.User;
import com.tripify.user_auth_service.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    // --- COMPANIONS ---
    @GetMapping("/companions")
    public ResponseEntity<List<CompanionDto>> getCompanions(Principal principal) {
        return ResponseEntity.ok(profileService.getCompanions(principal.getName()));
    }

    @PostMapping("/companions")
    public ResponseEntity<CompanionDto> addCompanion(@RequestBody CompanionDto companionDto, Principal principal) {
        return ResponseEntity.ok(profileService.addCompanion(principal.getName(), companionDto));
    }

    @DeleteMapping("/companions/{id}")
    public ResponseEntity<Void> deleteCompanion(@PathVariable UUID id, Principal principal) {
        profileService.deleteCompanion(principal.getName(), id);
        return ResponseEntity.ok().build();
    }

    // --- TRAVEL DOCUMENTS ---
    @GetMapping("/documents")
    public ResponseEntity<List<TravelDocumentDto>> getTravelDocuments(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(profileService.getTravelDocuments(user));
    }

    @PostMapping("/documents")
    public ResponseEntity<TravelDocumentDto> addTravelDocument(
            @AuthenticationPrincipal User user,
            @RequestBody TravelDocumentDto dto) {
        return ResponseEntity.ok(profileService.addTravelDocument(user, dto));
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> deleteTravelDocument(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        profileService.deleteTravelDocument(user, id);
        return ResponseEntity.ok().build();
    }

    // --- PAYMENT METHODS ---
    @GetMapping("/payments")
    public ResponseEntity<List<PaymentMethodDto>> getPayments(Principal principal) {
        return ResponseEntity.ok(profileService.getPaymentMethods(principal.getName()));
    }

    @PostMapping("/payments")
    public ResponseEntity<PaymentMethodDto> addPayment(@RequestBody PaymentMethodDto paymentMethodDto, Principal principal) {
        return ResponseEntity.ok(profileService.addPaymentMethod(principal.getName(), paymentMethodDto));
    }
}