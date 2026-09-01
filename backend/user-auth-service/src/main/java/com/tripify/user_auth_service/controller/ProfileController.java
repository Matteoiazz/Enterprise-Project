package com.tripify.user_auth_service.controller;

import com.tripify.user_auth_service.dto.request.CompanionDto;
import com.tripify.user_auth_service.dto.request.PaymentMethodDto;
import com.tripify.user_auth_service.dto.request.TravelDocumentDto;
import com.tripify.user_auth_service.service.ProfileService;
import jakarta.validation.Valid;
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

    @ModelAttribute
    public void syncKeycloakId(@AuthenticationPrincipal Jwt jwt) {
        if (jwt != null) {
            profileService.saveTrueKeycloakId(jwt.getClaimAsString("email"), jwt.getSubject());
        }
    }

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
        String email = jwt.getClaimAsString("email");
        String keycloakId = jwt.getSubject();

        com.tripify.user_auth_service.entity.User user = profileService.getUser(email);

        String displayNome = user.getName() != null ? user.getName() : "Utente";
        String displayCognome = user.getSurname() != null ? user.getSurname() : "";

        return ResponseEntity.ok(new com.tripify.user_auth_service.dto.response.UserResponse(
                keycloakId,
                displayNome, displayCognome, email,
                com.tripify.user_auth_service.service.ProfileService.normalizeImageUrl(user.getProfilePictureUrl()),
                user.getPhone(), user.getAddress(),
                user.getCompanyName(), user.getVatNumber(), user.getPec()
        ));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        String keycloakUserId = jwt.getSubject();

        profileService.deleteUserAccount(email);
        profileService.finalizeKeycloakDeletion(keycloakUserId, email);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me")
    public ResponseEntity<Void> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody com.tripify.user_auth_service.dto.request.UpdateProfileRequestDTO request) {

        String email = jwt.getClaimAsString("email");
        String keycloakUserId = jwt.getSubject();

        profileService.updateUserProfile(email, keycloakUserId, request);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/me/pec")
    public ResponseEntity<Void> updateMyPec(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody com.tripify.user_auth_service.dto.request.UpdatePecRequestDTO request) {

        String email = jwt.getClaimAsString("email");
        profileService.updatePec(email, request.getPec());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/me/picture")
    public ResponseEntity<java.util.Map<String, String>> uploadProfilePicture(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {

        String email = jwt.getClaimAsString("email");
        String imageUrl = profileService.uploadProfilePicture(email, file);

        return ResponseEntity.ok(java.util.Map.of("imageUrl", imageUrl));
    }

    @GetMapping("/organizers")
    public ResponseEntity<List<com.tripify.user_auth_service.dto.response.UserResponse>> getAllOrganizers(@AuthenticationPrincipal Jwt jwt) {
        String currentKeycloakId = jwt.getSubject();
        String currentEmail = jwt.getClaimAsString("email");

        List<com.tripify.user_auth_service.dto.response.UserResponse> organizers = profileService.getAllOrganizers().stream()
                .map(org -> {
                    if (org.email().equalsIgnoreCase(currentEmail)) {
                        return new com.tripify.user_auth_service.dto.response.UserResponse(
                                currentKeycloakId,
                                org.name(), org.surname(), org.email(), org.profilePictureUrl(),
                                org.phone(), org.address(), org.companyName(), org.vatNumber(), org.pec()
                        );
                    }
                    return org;
                }).toList();

        return ResponseEntity.ok(organizers);
    }

    @GetMapping("/organizers/{email}")
    public ResponseEntity<com.tripify.user_auth_service.dto.response.UserResponse> getOrganizerByEmail(@PathVariable String email) {
        return ResponseEntity.ok(profileService.getOrganizerById(email));
    }

    @GetMapping("/users/{id}/summary")
    public ResponseEntity<com.tripify.user_auth_service.dto.response.UserResponse> getUserSummary(@PathVariable String id) {
        return ResponseEntity.ok(profileService.getUserSummary(id));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}