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
import com.tripify.user_auth_service.exception.ResourceNotFoundException;
import com.tripify.user_auth_service.exception.UnauthorizedActionException;
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
import org.springframework.beans.factory.annotation.Value;
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


    @Value("${keycloak.admin.server-url:http://localhost:8180}")
    private String keycloakAdminServerUrl;

    @Value("${keycloak.admin.username:admin}")
    private String keycloakAdminUsername;

    @Value("${keycloak.admin.password:admin}")
    private String keycloakAdminPassword;

    private Keycloak getKeycloakAdminClient() {
        return KeycloakBuilder.builder()
                .serverUrl(keycloakAdminServerUrl)
                .realm("master")
                .clientId("admin-cli")
                .grantType(org.keycloak.OAuth2Constants.PASSWORD)
                .username(keycloakAdminUsername)
                .password(keycloakAdminPassword)
                .build();
    }

    public User getUser(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return createAndSyncUserFromKeycloak(email);
        }

        if (user.getName() == null || user.getSurname() == null || user.getPhone() == null) {
            user = backfillMissingProfileFieldsFromKeycloak(user, email);
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
                    String company = getAttributeValue(kcUser, "companyName");
                    String vat = getAttributeValue(kcUser, "vatNumber");
                    String pec = getAttributeValue(kcUser, "pec");

                    String businessDataError = validateBusinessData(company, vat, pec);
                    if (businessDataError != null) {
                        log.warn("Richiesta ruolo ROLE_ORGANIZER respinta per {}: {}", email, businessDataError);
                        return user;
                    }

                    user.setRole(Role.ROLE_ORGANIZER);
                    user.setCompanyName(company);
                    user.setVatNumber(vat);
                    user.setPec(pec);

                    try {
                        RoleRepresentation organizerRole = keycloak.realm("tripify").roles().get("ROLE_ORGANIZER").toRepresentation();
                        keycloak.realm("tripify").users().get(kcUser.getId()).roles().realmLevel().add(List.of(organizerRole));
                        log.info("Ruolo ROLE_ORGANIZER assegnato con successo su Keycloak per l'utente {}", email);

                        return userRepository.save(user);
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

    private String validateBusinessData(String companyName, String vatNumber, String pec) {
        if (companyName == null || companyName.trim().length() < 2 || companyName.trim().length() > 255) {
            return "ragione sociale non valida";
        }
        if (vatNumber == null || !isValidItalianVatNumber(vatNumber.trim())) {
            return "partita IVA non valida";
        }
        if (pec == null || !pec.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return "PEC non valida";
        }
        return null;
    }

    private boolean isValidItalianVatNumber(String vatNumber) {
        String cleaned = vatNumber.toUpperCase().startsWith("IT") ? vatNumber.substring(2) : vatNumber;
        if (!cleaned.matches("\\d{11}")) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            int digit = cleaned.charAt(i) - '0';
            if (i % 2 == 0) {
                sum += digit;
            } else {
                int doubled = digit * 2;
                sum += doubled > 9 ? doubled - 9 : doubled;
            }
        }
        int checkDigit = (10 - (sum % 10)) % 10;
        return checkDigit == (cleaned.charAt(10) - '0');
    }

    private User backfillMissingProfileFieldsFromKeycloak(User user, String email) {
        try {
            Keycloak keycloak = getKeycloakAdminClient();
            List<UserRepresentation> matches = keycloak.realm("tripify").users().searchByEmail(email, true);

            if (!matches.isEmpty()) {
                UserRepresentation kcUser = matches.get(0);
                boolean changed = false;

                if (user.getName() == null && kcUser.getFirstName() != null && !kcUser.getFirstName().isBlank()) {
                    user.setName(kcUser.getFirstName());
                    changed = true;
                }
                if (user.getSurname() == null && kcUser.getLastName() != null && !kcUser.getLastName().isBlank()) {
                    user.setSurname(kcUser.getLastName());
                    changed = true;
                }
                if (user.getPhone() == null) {
                    String phone = getAttributeValue(kcUser, "phoneNumber");
                    if (phone != null) {
                        user.setPhone(phone);
                        changed = true;
                    }
                }

                if (changed) {
                    return userRepository.save(user);
                }
            }
        } catch (Exception e) {
            log.warn("Impossibile completare i dati profilo mancanti da Keycloak per {}: {}", email, e.getMessage());
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

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<CompanionDto> getCompanions(String userEmail) {
        return companionRepository.findByUser(getUser(userEmail)).stream()
                .map(c -> CompanionDto.builder().id(c.getId()).firstName(c.getFirstName())
                        .lastName(c.getLastName()).dateOfBirth(c.getDateOfBirth()).build())
                .collect(Collectors.toList());
    }

    public CompanionDto addCompanion(String userEmail, CompanionDto dto) {
        if (dto.getFirstName() == null || dto.getFirstName().isBlank()) {
            throw new IllegalArgumentException("Il nome del compagno è obbligatorio");
        }
        if (dto.getLastName() == null || dto.getLastName().isBlank()) {
            throw new IllegalArgumentException("Il cognome del compagno è obbligatorio");
        }
        if (dto.getDateOfBirth() == null) {
            throw new IllegalArgumentException("La data di nascita è obbligatoria");
        }
        if (dto.getDateOfBirth().isAfter(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("La data di nascita non può essere nel futuro");
        }
        if (java.time.Period.between(dto.getDateOfBirth(), java.time.LocalDate.now()).getYears() < 18) {
            throw new IllegalArgumentException("Il compagno di viaggio deve essere maggiorenne (almeno 18 anni)");
        }

        Companion saved = companionRepository.save(Companion.builder()
                .firstName(dto.getFirstName()).lastName(dto.getLastName())
                .dateOfBirth(dto.getDateOfBirth()).user(getUser(userEmail)).build());
        return CompanionDto.builder().id(saved.getId()).firstName(saved.getFirstName())
                .lastName(saved.getLastName()).dateOfBirth(saved.getDateOfBirth()).build();
    }

    public void deleteCompanion(String userEmail, UUID companionId) {
        Companion companion = companionRepository.findById(companionId)
                .orElseThrow(() -> new ResourceNotFoundException("Compagno non trovato"));

        if (!companion.getUser().getId().equals(getUser(userEmail).getId())) {
            throw new UnauthorizedActionException("Non autorizzato");
        }
        companionRepository.delete(companion);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
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
        if (dto.getDocumentType() == null || dto.getDocumentType().isBlank()) {
            throw new IllegalArgumentException("Il tipo di documento è obbligatorio");
        }
        if (dto.getDocumentNumber() == null || dto.getDocumentNumber().isBlank()) {
            throw new IllegalArgumentException("Il numero di documento è obbligatorio");
        }
        if (dto.getIssuingCountry() == null || dto.getIssuingCountry().isBlank()) {
            throw new IllegalArgumentException("Il paese di rilascio è obbligatorio");
        }
        if (dto.getExpirationDate() == null) {
            throw new IllegalArgumentException("La data di scadenza è obbligatoria");
        }
        if (dto.getExpirationDate().isBefore(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("Il documento è già scaduto");
        }

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
                .orElseThrow(() -> new ResourceNotFoundException("Documento non trovato"));

        if (!doc.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("Non autorizzato");
        }
        documentRepository.delete(doc);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<PaymentMethodDto> getPaymentMethods(String userEmail) {
        return paymentMethodRepository.findByUser(getUser(userEmail)).stream()
                .map(p -> PaymentMethodDto.builder().id(p.getId()).cardProvider(p.getCardProvider())
                        .lastFourDigits(p.getLastFourDigits()).expirationMonthYear(p.getExpirationMonthYear()).build())
                .collect(Collectors.toList());
    }

    public PaymentMethodDto addPaymentMethod(String userEmail, PaymentMethodDto dto) {
        String cardNumber = dto.getCardNumber() != null ? dto.getCardNumber().replaceAll("\\s+", "") : null;
        if (cardNumber == null || cardNumber.length() < 13 || cardNumber.length() > 19 || !cardNumber.matches("\\d+")) {
            throw new IllegalArgumentException("Numero carta non valido");
        }
        if (dto.getExpirationMonthYear() == null || dto.getExpirationMonthYear().isBlank()) {
            throw new IllegalArgumentException("La scadenza della carta è obbligatoria");
        }
        try {
            java.time.YearMonth expiry = java.time.YearMonth.parse(dto.getExpirationMonthYear(),
                    java.time.format.DateTimeFormatter.ofPattern("MM/yy"));
            if (expiry.isBefore(java.time.YearMonth.now())) {
                throw new IllegalArgumentException("La carta è scaduta");
            }
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("Formato scadenza non valido, usa MM/AA");
        }

        String lastFour = cardNumber.substring(cardNumber.length() - 4);

        PaymentMethod saved = paymentMethodRepository.save(PaymentMethod.builder()
                .cardProvider(dto.getCardProvider()).lastFourDigits(lastFour)
                .expirationMonthYear(dto.getExpirationMonthYear()).user(getUser(userEmail)).build());

        return PaymentMethodDto.builder().id(saved.getId()).cardProvider(saved.getCardProvider())
                .lastFourDigits(saved.getLastFourDigits()).expirationMonthYear(saved.getExpirationMonthYear()).build();
    }

    public void deletePaymentMethod(String userEmail, UUID id) {
        User user = getUser(userEmail);
        PaymentMethod method = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Metodo di pagamento non trovato"));

        if (!method.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("Non autorizzato");
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

        if (request.getName() != null || request.getSurname() != null || request.getEmail() != null || request.getPhone() != null) {
            org.keycloak.representations.idm.UserRepresentation kcUser = userResource.toRepresentation();
            if (request.getName() != null) kcUser.setFirstName(request.getName());
            if (request.getSurname() != null) kcUser.setLastName(request.getSurname());
            if (request.getEmail() != null) {
                kcUser.setEmail(request.getEmail());
                kcUser.setUsername(request.getEmail());
            }
            if (request.getPhone() != null) {
                if (kcUser.getAttributes() == null) {
                    kcUser.setAttributes(new java.util.HashMap<>());
                }
                kcUser.getAttributes().put("phoneNumber", java.util.List.of(request.getPhone()));
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


    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<com.tripify.user_auth_service.dto.response.UserResponse> getAllOrganizers() {
        return userRepository.findByRole(Role.ROLE_ORGANIZER).stream()
                .map(u -> new com.tripify.user_auth_service.dto.response.UserResponse(
                        (u.getUsername() != null && !u.getUsername().isEmpty()) ? u.getUsername() : u.getId().toString(),
                        u.getName() != null ? u.getName() : "Organizzatore",
                        u.getSurname() != null ? u.getSurname() : "",
                        u.getEmail(),
                        u.getProfilePictureUrl(),
                        u.getPhone(),
                        u.getAddress(),
                        u.getCompanyName(),
                        u.getVatNumber(),
                        u.getPec()
                ))
                .toList();
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public com.tripify.user_auth_service.dto.response.UserResponse getOrganizerById(String identifier) {
        User u;
        if (identifier.contains("@")) {
            u = getUser(identifier);
        } else {
            u = userRepository.findByUsername(identifier)
                    .or(() -> {
                        try {
                            return userRepository.findById(UUID.fromString(identifier));
                        } catch (IllegalArgumentException notAUuid) {
                            return java.util.Optional.empty();
                        }
                    })
                    .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato nel database locale"));
        }

        if (u.getRole() != Role.ROLE_ORGANIZER) {
            throw new ResourceNotFoundException("L'utente richiesto non è un organizzatore");
        }

        String kcId = (u.getUsername() != null && !u.getUsername().isEmpty()) ? u.getUsername() : u.getId().toString();

        return new com.tripify.user_auth_service.dto.response.UserResponse(
                kcId,
                u.getName() != null ? u.getName() : "Organizzatore",
                u.getSurname() != null ? u.getSurname() : "",
                u.getEmail(),
                u.getProfilePictureUrl(),
                u.getPhone(),
                u.getAddress(),
                u.getCompanyName(),
                u.getVatNumber(),
                u.getPec()
        );
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public com.tripify.user_auth_service.dto.response.UserResponse getUserSummary(String identifier) {
        User u;
        if (identifier.contains("@")) {
            u = getUser(identifier);
        } else {
            u = userRepository.findByUsername(identifier)
                    .or(() -> {
                        try {
                            return userRepository.findById(UUID.fromString(identifier));
                        } catch (IllegalArgumentException notAUuid) {
                            return java.util.Optional.empty();
                        }
                    })
                    .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));
        }

        String kcId = (u.getUsername() != null && !u.getUsername().isEmpty()) ? u.getUsername() : u.getId().toString();

        return new com.tripify.user_auth_service.dto.response.UserResponse(
                kcId,
                u.getName() != null ? u.getName() : "Utente",
                u.getSurname() != null ? u.getSurname() : "",
                u.getEmail(),
                u.getProfilePictureUrl(),
                u.getPhone(),
                u.getAddress(),
                u.getCompanyName(),
                u.getVatNumber(),
                u.getPec()
        );
    }

    public User getUserWithKeycloakId(String email, String keycloakId) {
        User user = getUser(email);
        if (keycloakId != null && !keycloakId.equals(user.getUsername())) {
            user.setUsername(keycloakId);
            user = userRepository.save(user);
        }
        return user;
    }

    public void saveTrueKeycloakId(String email, String kcId) {
        User u = userRepository.findByEmail(email).orElse(null);
        if (u == null) return;

        if (kcId != null && !kcId.equals(u.getUsername())) {
            u.setUsername(kcId);
            userRepository.save(u);
        }
    }
}