package com.tripify.user_auth_service.service;

import com.cloudinary.Cloudinary;
import com.tripify.user_auth_service.dto.request.CompanionDto;
import com.tripify.user_auth_service.dto.request.PaymentMethodDto;
import com.tripify.user_auth_service.dto.request.TravelDocumentDto;
import com.tripify.user_auth_service.entity.Companion;
import com.tripify.user_auth_service.entity.PaymentMethod;
import com.tripify.user_auth_service.entity.Role;
import com.tripify.user_auth_service.entity.TravelDocument;
import com.tripify.user_auth_service.dto.request.UpdateProfileRequestDTO;
import com.tripify.user_auth_service.entity.User;
import com.tripify.user_auth_service.repository.CompanionRepository;
import com.tripify.user_auth_service.repository.PaymentMethodRepository;
import com.tripify.user_auth_service.repository.TravelDocumentRepository;
import com.tripify.user_auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final UserRepository userRepository;
    private final CompanionRepository companionRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final TravelDocumentRepository documentRepository;
    private final Cloudinary cloudinary;

    private Keycloak getKeycloakAdminClient() {
        return KeycloakBuilder.builder()
                .serverUrl("http://localhost:8180")
                .realm("master")
                .clientId("admin-cli")
                .grantType(org.keycloak.OAuth2Constants.PASSWORD)
                .username("admin")
                .password("admin")
                .build();
    }

    public User getUser(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return createAndSyncUserFromKeycloak(email);
        }

        if (user.getRole() == Role.ROLE_TRAVELER) {
            return syncRoleFromKeycloak(user, email);
        }

        return user;
    }

    private String getAttributeValue(UserRepresentation kcUser, String key) {
        if (kcUser.getAttributes() == null) return null;

        String[] possibleKeys = { key, "profile.attributes." + key, "user.attributes." + key };
        for (String k : possibleKeys) {
            List<String> values = kcUser.getAttributes().get(k);
            if (values != null && !values.isEmpty() && values.get(0) != null && !values.get(0).trim().isEmpty()) {
                return values.get(0);
            }
        }
        return null;
    }

    private User syncRoleFromKeycloak(User user, String email) {
        try {
            Keycloak keycloak = getKeycloakAdminClient();
            List<UserRepresentation> matches = keycloak.realm("tripify").users().searchByEmail(email, true);

            if (!matches.isEmpty()) {
                UserRepresentation kcUser = matches.get(0);
                String userType = getAttributeValue(kcUser, "userType");

                if (userType != null && userType.toLowerCase().contains("organizer")) {
                    user.setRole(Role.ROLE_ORGANIZER);

                    String company = getAttributeValue(kcUser, "companyName");
                    if (company != null) user.setCompanyName(company);

                    String vat = getAttributeValue(kcUser, "vatNumber");
                    if (vat != null) user.setVatNumber(vat);

                    String pec = getAttributeValue(kcUser, "pec");
                    if (pec != null) user.setPec(pec);

                    try {
                        RoleRepresentation organizerRole = keycloak.realm("tripify").roles().get("ROLE_ORGANIZER").toRepresentation();
                        keycloak.realm("tripify").users().get(kcUser.getId()).roles().realmLevel().add(List.of(organizerRole));
                        log.info("Ruolo ROLE_ORGANIZER assegnato con successo su Keycloak per l'utente {}", email);

                        return userRepository.save(user); // Salva sul DB locale solo se Keycloak ha successo
                    } catch (Exception roleAssignmentFailed) {
                        log.warn("Impossibile assegnare il ruolo realm ROLE_ORGANIZER a {}: {}", email, roleAssignmentFailed.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Sincronizzazione ruolo fallita per {}: {}", email, e.getMessage());
        }
        return user;
    }

    private User createAndSyncUserFromKeycloak(String email) {
        User.UserBuilder userBuilder = User.builder()
                .email(email)
                .password("MANAGED_BY_KEYCLOAK")
                .role(Role.ROLE_TRAVELER);

        try {
            Keycloak keycloak = getKeycloakAdminClient();
            List<UserRepresentation> matches = keycloak.realm("tripify").users().searchByEmail(email, true);

            if (!matches.isEmpty()) {
                UserRepresentation kcUser = matches.get(0);
                userBuilder.name(kcUser.getFirstName());
                userBuilder.surname(kcUser.getLastName());

                String phone = getAttributeValue(kcUser, "phoneNumber");
                if (phone != null) userBuilder.phone(phone);
            }
        } catch (Exception e) {
            log.warn("Impossibile contattare Keycloak per l'utente {}, uso i valori di default: {}", email, e.getMessage());
        }

        User savedUser = userRepository.save(userBuilder.build());
        return syncRoleFromKeycloak(savedUser, email);
    }

    public List<CompanionDto> getCompanions(String userEmail) {
        return companionRepository.findByUser(getUser(userEmail)).stream()
                .map(c -> CompanionDto.builder().id(c.getId()).firstName(c.getFirstName())
                        .lastName(c.getLastName()).dateOfBirth(c.getDateOfBirth()).build())
                .collect(Collectors.toList());
    }

    public CompanionDto addCompanion(String userEmail, CompanionDto dto) {
        Companion saved = companionRepository.save(Companion.builder()
                .firstName(dto.getFirstName()).lastName(dto.getLastName())
                .dateOfBirth(dto.getDateOfBirth()).user(getUser(userEmail)).build());
        return CompanionDto.builder().id(saved.getId()).firstName(saved.getFirstName())
                .lastName(saved.getLastName()).dateOfBirth(saved.getDateOfBirth()).build();
    }

    public void deleteCompanion(String userEmail, UUID companionId) {
        Companion companion = companionRepository.findById(companionId)
                .orElseThrow(() -> new RuntimeException("Compagno non trovato"));

        if (!companion.getUser().getId().equals(getUser(userEmail).getId())) {
            throw new RuntimeException("Non autorizzato");
        }
        companionRepository.delete(companion);
    }

    public List<TravelDocumentDto> getTravelDocuments(String userEmail) {
        User user = getUser(userEmail);
        return documentRepository.findByUser_Id(user.getId()).stream()
                .map(doc -> TravelDocumentDto.builder()
                        .id(doc.getId())
                        .documentType(doc.getDocumentType())
                        .documentNumber(doc.getDocumentNumber())
                        .expirationDate(doc.getExpirationDate())
                        .issuingCountry(doc.getIssuingCountry())
                        .build())
                .collect(Collectors.toList());
    }

    public TravelDocumentDto addTravelDocument(String userEmail, TravelDocumentDto dto) {
        User user = getUser(userEmail);
        TravelDocument doc = TravelDocument.builder()
                .user(user)
                .documentType(dto.getDocumentType())
                .documentNumber(dto.getDocumentNumber())
                .expirationDate(dto.getExpirationDate())
                .issuingCountry(dto.getIssuingCountry())
                .build();

        doc = documentRepository.save(doc);
        dto.setId(doc.getId());
        return dto;
    }

    public void deleteTravelDocument(String userEmail, UUID id) {
        User user = getUser(userEmail);
        TravelDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Documento non trovato"));

        if (!doc.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Non autorizzato");
        }
        documentRepository.delete(doc);
    }

    public List<PaymentMethodDto> getPaymentMethods(String userEmail) {
        return paymentMethodRepository.findByUser(getUser(userEmail)).stream()
                .map(p -> PaymentMethodDto.builder().id(p.getId()).cardProvider(p.getCardProvider())
                        .lastFourDigits(p.getLastFourDigits()).expirationMonthYear(p.getExpirationMonthYear()).build())
                .collect(Collectors.toList());
    }

    public PaymentMethodDto addPaymentMethod(String userEmail, PaymentMethodDto dto) {
        String lastFour = (dto.getCardNumber() != null && dto.getCardNumber().length() >= 4)
                ? dto.getCardNumber().substring(dto.getCardNumber().length() - 4)
                : "0000";

        PaymentMethod saved = paymentMethodRepository.save(PaymentMethod.builder()
                .cardProvider(dto.getCardProvider()).lastFourDigits(lastFour)
                .expirationMonthYear(dto.getExpirationMonthYear()).user(getUser(userEmail)).build());

        return PaymentMethodDto.builder().id(saved.getId()).cardProvider(saved.getCardProvider())
                .lastFourDigits(saved.getLastFourDigits()).expirationMonthYear(saved.getExpirationMonthYear()).build();
    }

    public void deletePaymentMethod(String userEmail, UUID id) {
        User user = getUser(userEmail);
        PaymentMethod method = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Metodo di pagamento non trovato"));

        if (!method.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Non autorizzato");
        }
        paymentMethodRepository.delete(method);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteUserAccount(String email, String keycloakUserId) {
        User user = getUser(email);

        companionRepository.deleteAll(companionRepository.findByUser(user));
        paymentMethodRepository.deleteAll(paymentMethodRepository.findByUser(user));
        documentRepository.deleteAll(documentRepository.findByUser_Id(user.getId()));
        userRepository.delete(user);

        try {
            Keycloak keycloak = getKeycloakAdminClient();
            keycloak.realm("tripify").users().delete(keycloakUserId);
            log.info("Account eliminato definitivamente sia in locale che su Keycloak per: {}", email);
        } catch (Exception e) {
            log.warn("Errore eliminazione utente su Keycloak, ma i dati locali sono stati distrutti con successo: {}", e.getMessage());
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public User updateUserProfile(String email, String keycloakUserId, com.tripify.user_auth_service.dto.request.UpdateProfileRequestDTO request) {
        User user = getUser(email);

        if (request.getName() != null) user.setName(request.getName());
        if (request.getSurname() != null) user.setSurname(request.getSurname());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getEmail() != null) user.setEmail(request.getEmail());

        userRepository.save(user);

        Keycloak keycloak = getKeycloakAdminClient();
        UserResource userResource = keycloak.realm("tripify").users().get(keycloakUserId);

        if (request.getName() != null || request.getSurname() != null || request.getEmail() != null) {
            org.keycloak.representations.idm.UserRepresentation kcUser = userResource.toRepresentation();
            if (request.getName() != null) kcUser.setFirstName(request.getName());
            if (request.getSurname() != null) kcUser.setLastName(request.getSurname());
            if (request.getEmail() != null) {
                kcUser.setEmail(request.getEmail());
                kcUser.setUsername(request.getEmail());
            }
            userResource.update(kcUser);
        }

        if (request.getNewPassword() != null && !request.getNewPassword().isEmpty()) {
            org.keycloak.representations.idm.CredentialRepresentation passwordCred = new org.keycloak.representations.idm.CredentialRepresentation();
            passwordCred.setTemporary(false);
            passwordCred.setType(org.keycloak.representations.idm.CredentialRepresentation.PASSWORD);
            passwordCred.setValue(request.getNewPassword());

            userResource.resetPassword(passwordCred);
        }

        return user;
    }

    @org.springframework.transaction.annotation.Transactional
    public String uploadProfilePicture(String email, org.springframework.web.multipart.MultipartFile file) {
        User user = getUser(email);
        try {
            java.util.Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    com.cloudinary.utils.ObjectUtils.asMap("folder", "tripify_profiles"));

            String imageUrl = uploadResult.get("url").toString();

            user.setProfilePictureUrl(imageUrl);
            userRepository.save(user);

            return imageUrl;
        } catch (java.io.IOException e) {
            throw new RuntimeException("Errore durante l'upload dell'immagine", e);
        }
    }
}