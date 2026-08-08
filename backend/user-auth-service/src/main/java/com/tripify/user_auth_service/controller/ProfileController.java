package com.tripify.user_auth_service.controller;

import com.tripify.user_auth_service.dto.request.CompanionDto;
import com.tripify.user_auth_service.dto.request.PaymentMethodDto;
import com.tripify.user_auth_service.dto.request.TravelDocumentDto;
import com.tripify.user_auth_service.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

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

    // --- TRAVEL DOCUMENTS ---
    @GetMapping("/documents")
    public ResponseEntity<List<TravelDocumentDto>> getDocuments(Principal principal) {
        return ResponseEntity.ok(profileService.getTravelDocuments(principal.getName()));
    }

    @PostMapping("/documents")
    public ResponseEntity<TravelDocumentDto> addDocument(@RequestBody TravelDocumentDto documentDto, Principal principal) {
        return ResponseEntity.ok(profileService.addTravelDocument(principal.getName(), documentDto));
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