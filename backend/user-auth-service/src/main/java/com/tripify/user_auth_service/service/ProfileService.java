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

import java.util.Collection;
import java.util.List;
import java.util.Map;
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

    private static final long KEYCLOAK_SYNC_TTL_MINUTES = 10;


    @Value("${keycloak.admin.server-url:http://localhost:8180}")
    private String keycloakAdminServerUrl;

    @Value("${keycloak.admin.username:admin}")
    private String keycloakAdminUsername;

    @Value("${keycloak.admin.password:admin}")
    private String keycloakAdminPassword;

    private volatile Keycloak keycloakAdminClient;

    private Keycloak getKeycloakAdminClient() {
        Keycloak client = keycloakAdminClient;
        if (client == null) {
            synchronized (this) {
                client = keycloakAdminClient;
                if (client == null) {
                    client = KeycloakBuilder.builder()
                            .serverUrl(keycloakAdminServerUrl)
                            .realm("master")
                            .clientId("admin-cli")
                            .grantType(org.keycloak.OAuth2Constants.PASSWORD)
                            .username(keycloakAdminUsername)
                            .password(keycloakAdminPassword)
                            .build();
                    keycloakAdminClient = client;
                }
            }
        }
        return client;
    }

    public User getUser(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return createAndSyncUserFromKeycloak(email);
        }

        java.time.Instant now = java.time.Instant.now();
        boolean syncDue = user.getLastSyncedAt() == null
                || user.getLastSyncedAt().isBefore(now.minus(java.time.Duration.ofMinutes(KEYCLOAK_SYNC_TTL_MINUTES)));
        if (!syncDue) {
            return user;
        }

        if (user.getName() == null || user.getSurname() == null || user.getPhone() == null) {
            user = backfillMissingProfileFieldsFromKeycloak(user, email);
        }

        if (user.getRole() == Role.ROLE_TRAVELER) {
            user = syncRoleFromKeycloak(user, email);
        }

        user.setLastSyncedAt(now);
        return userRepository.save(user);
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
        if (pec == null || pec.trim().length() > 255 || !pec.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
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

        User savedUser;
        try {
            savedUser = userRepository.save(userBuilder.build());
        } catch (org.springframework.dao.DataIntegrityViolationException raceLost) {
            savedUser = userRepository.findByEmail(email).orElseThrow(() -> raceLost);
        }
        User synced = syncRoleFromKeycloak(savedUser, email);
        synced.setLastSyncedAt(java.time.Instant.now());
        return userRepository.save(synced);
    }

    @org.springframework.transaction.annotation.Transactional
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

    @org.springframework.transaction.annotation.Transactional
    public List<TravelDocumentDto> getTravelDocuments(String userEmail) {
        User user = getUser(userEmail);
        return documentRepository.findByUser_Id(user.getId()).stream()
                .map(doc -> TravelDocumentDto.builder()
                        .id(doc.getId())
                        .documentType(doc.getDocumentType())
                        .documentNumber(maskDocumentNumber(doc.getDocumentNumber()))
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
        dto.setDocumentNumber(maskDocumentNumber(doc.getDocumentNumber()));
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

    @org.springframework.transaction.annotation.Transactional
    public List<PaymentMethodDto> getPaymentMethods(String userEmail) {
        return paymentMethodRepository.findByUser(getUser(userEmail)).stream()
                .sorted((a, b) -> Boolean.compare(b.isDefault(), a.isDefault()))
                .map(this::toPaymentMethodDto)
                .collect(Collectors.toList());
    }

    private PaymentMethodDto toPaymentMethodDto(PaymentMethod p) {
        return PaymentMethodDto.builder().id(p.getId()).cardProvider(p.getCardProvider())
                .lastFourDigits(p.getLastFourDigits()).expirationMonthYear(p.getExpirationMonthYear())
                .defaultCard(p.isDefault()).build();
    }

    @org.springframework.transaction.annotation.Transactional
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

        User user = getUser(userEmail);
        boolean firstCard = paymentMethodRepository.findByUser(user).isEmpty();
        String lastFour = cardNumber.substring(cardNumber.length() - 4);

        PaymentMethod saved = paymentMethodRepository.save(PaymentMethod.builder()
                .cardProvider(dto.getCardProvider()).lastFourDigits(lastFour)
                .expirationMonthYear(dto.getExpirationMonthYear()).isDefault(firstCard).user(user).build());

        return toPaymentMethodDto(saved);
    }

    @org.springframework.transaction.annotation.Transactional
    public void setDefaultPaymentMethod(String userEmail, UUID id) {
        User user = getUser(userEmail);
        PaymentMethod target = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Metodo di pagamento non trovato"));
        if (!target.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("Non autorizzato");
        }
        for (PaymentMethod pm : paymentMethodRepository.findByUser(user)) {
            boolean shouldBeDefault = pm.getId().equals(id);
            if (pm.isDefault() != shouldBeDefault) {
                pm.setDefault(shouldBeDefault);
                paymentMethodRepository.save(pm);
            }
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public void deletePaymentMethod(String userEmail, UUID id) {
        User user = getUser(userEmail);
        PaymentMethod method = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Metodo di pagamento non trovato"));

        if (!method.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("Non autorizzato");
        }
        boolean wasDefault = method.isDefault();
        paymentMethodRepository.delete(method);

        if (wasDefault) {
            paymentMethodRepository.findByUser(user).stream()
                    .filter(pm -> !pm.getId().equals(id))
                    .findFirst()
                    .ifPresent(pm -> {
                        pm.setDefault(true);
                        paymentMethodRepository.save(pm);
                    });
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteUserAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        companionRepository.deleteAll(companionRepository.findByUser(user));
        paymentMethodRepository.deleteAll(paymentMethodRepository.findByUser(user));
        documentRepository.deleteAll(documentRepository.findByUser_Id(user.getId()));
        userRepository.delete(user);

        log.info("Dati account rimossi in locale per: {}", email);
    }

    public void finalizeKeycloakDeletion(String keycloakUserId, String email) {
        try {
            getKeycloakAdminClient().realm("tripify").users().delete(keycloakUserId);
            log.info("Identita' Keycloak eliminata per: {}", email);
            return;
        } catch (Exception keycloakDeleteFailed) {
            log.error("Dati locali gia' rimossi per {} ma la cancellazione su Keycloak e' fallita: {}",
                    email, keycloakDeleteFailed.getMessage());
        }

        if (!disableKeycloakUser(keycloakUserId)) {
            throw new IllegalStateException(
                    "I dati dell'account sono stati rimossi ma non e' stato possibile revocare l'accesso. Contatta il supporto.");
        }
        log.warn("Accesso Keycloak revocato per {} tramite disabilitazione dopo il fallimento della cancellazione", email);
    }

    private boolean disableKeycloakUser(String keycloakUserId) {
        try {
            UserResource kcUser = getKeycloakAdminClient().realm("tripify").users().get(keycloakUserId);
            UserRepresentation rep = kcUser.toRepresentation();
            rep.setEnabled(false);
            kcUser.update(rep);
            return true;
        } catch (Exception disableFailed) {
            log.error("Impossibile disabilitare l'utente Keycloak {}: {}", keycloakUserId, disableFailed.getMessage());
            return false;
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public User updateUserProfile(String email, String keycloakUserId, com.tripify.user_auth_service.dto.request.UpdateProfileRequestDTO request) {
        boolean changingPassword = request.getNewPassword() != null && !request.getNewPassword().isEmpty();
        if (changingPassword) {
            verifyCurrentPassword(email, request.getCurrentPassword());
        }

        User user = getUser(email);

        boolean emailChanged = request.getEmail() != null && !request.getEmail().equalsIgnoreCase(user.getEmail());
        if (emailChanged && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email già in uso da un altro account");
        }

        Keycloak keycloak = getKeycloakAdminClient();
        UserResource userResource = keycloak.realm("tripify").users().get(keycloakUserId);

        if (request.getName() != null) user.setName(request.getName());
        if (request.getSurname() != null) user.setSurname(request.getSurname());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (emailChanged) user.setEmail(request.getEmail());

        userRepository.save(user);

        if (request.getName() != null || request.getSurname() != null || emailChanged || request.getPhone() != null) {
            org.keycloak.representations.idm.UserRepresentation kcUser = userResource.toRepresentation();
            if (request.getName() != null) kcUser.setFirstName(request.getName());
            if (request.getSurname() != null) kcUser.setLastName(request.getSurname());
            if (emailChanged) {
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

        if (changingPassword) {
            org.keycloak.representations.idm.CredentialRepresentation passwordCred = new org.keycloak.representations.idm.CredentialRepresentation();
            passwordCred.setTemporary(false);
            passwordCred.setType(org.keycloak.representations.idm.CredentialRepresentation.PASSWORD);
            passwordCred.setValue(request.getNewPassword());

            try {
                userResource.resetPassword(passwordCred);
            } catch (jakarta.ws.rs.WebApplicationException passwordRejected) {
                int status = passwordRejected.getResponse() != null ? passwordRejected.getResponse().getStatus() : 0;
                if (status >= 400 && status < 500) {
                    throw new IllegalArgumentException("La nuova password non rispetta i requisiti di sicurezza richiesti");
                }
                throw passwordRejected;
            }
        }

        return user;
    }

    private void verifyCurrentPassword(String currentEmail, String currentPassword) {
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("Inserisci la password attuale per poterla cambiare");
        }
        if (!currentPasswordMatches(currentEmail, currentPassword)) {
            throw new IllegalArgumentException("La password attuale non è corretta");
        }
    }

    boolean currentPasswordMatches(String currentEmail, String currentPassword) {
        String tokenEndpoint = keycloakAdminServerUrl + "/realms/tripify/protocol/openid-connect/token";
        String form = "grant_type=password&client_id=tripify-android-client"
                + "&username=" + java.net.URLEncoder.encode(currentEmail, java.nio.charset.StandardCharsets.UTF_8)
                + "&password=" + java.net.URLEncoder.encode(currentPassword, java.nio.charset.StandardCharsets.UTF_8);

        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(5))
                    .build();
            java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(tokenEndpoint))
                    .timeout(java.time.Duration.ofSeconds(5))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(form))
                    .build();

            java.net.http.HttpResponse<String> response =
                    client.send(httpRequest, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return true;
            }

            String body = response.body() != null ? response.body() : "";
            if (response.statusCode() == 401 || body.contains("invalid_grant")) {
                return false;
            }

            log.error("Verifica password non riuscita: HTTP {} dal token endpoint Keycloak ({}). "
                            + "Probabilmente 'Direct Access Grants' e' disabilitato sul client tripify-android-client.",
                    response.statusCode(), body);
            throw new IllegalStateException("Verifica della password temporaneamente non disponibile");

        } catch (java.io.IOException networkError) {
            log.warn("Verifica password: errore di rete verso Keycloak: {}", networkError.getMessage());
            throw new IllegalStateException("Impossibile verificare la password attuale, riprova piu' tardi");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Verifica della password interrotta, riprova");
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public void updatePec(String email, String pec) {
        User user = getUser(email);
        if (user.getRole() != Role.ROLE_ORGANIZER) {
            throw new UnauthorizedActionException("Solo gli organizzatori possono modificare la PEC");
        }
        if (pec == null || pec.trim().length() > 255 || !pec.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("PEC non valida");
        }
        user.setPec(pec.trim());
        userRepository.save(user);
    }

    @org.springframework.transaction.annotation.Transactional
    public String uploadProfilePicture(String email, org.springframework.web.multipart.MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Il file deve essere un'immagine");
        }
        User user = getUser(email);
        String previousUrl = user.getProfilePictureUrl();
        try {
            java.util.Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    com.cloudinary.utils.ObjectUtils.asMap("folder", "tripify_profiles"));

            Object secure = uploadResult.get("secure_url");
            String imageUrl = normalizeImageUrl((secure != null ? secure : uploadResult.get("url")).toString());

            user.setProfilePictureUrl(imageUrl);
            userRepository.save(user);

            deleteCloudinaryImageQuietly(previousUrl);

            return imageUrl;
        } catch (java.io.IOException e) {
            throw new RuntimeException("Errore durante l'upload dell'immagine", e);
        }
    }

    private void deleteCloudinaryImageQuietly(String url) {
        String publicId = cloudinaryPublicId(url);
        if (publicId == null) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, com.cloudinary.utils.ObjectUtils.emptyMap());
        } catch (Exception e) {
            log.warn("Vecchia immagine profilo non rimossa da Cloudinary ({}): {}", publicId, e.getMessage());
        }
    }

    static String cloudinaryPublicId(String url) {
        if (url == null || !url.contains("res.cloudinary.com") || !url.contains("/upload/")) {
            return null;
        }
        String path = url.substring(url.indexOf("/upload/") + "/upload/".length());
        int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        if (path.matches("v\\d+/.*")) {
            path = path.substring(path.indexOf('/') + 1);
        }
        int lastSlash = path.lastIndexOf('/');
        int lastDot = path.lastIndexOf('.');
        if (lastDot > lastSlash) {
            path = path.substring(0, lastDot);
        }
        return path.isBlank() ? null : path;
    }

    public static String normalizeImageUrl(String url) {
        if (url != null && url.startsWith("http://") && url.contains("cloudinary.com")) {
            return "https://" + url.substring("http://".length());
        }
        return url;
    }

    private static String maskDocumentNumber(String number) {
        if (number == null) {
            return null;
        }
        String trimmed = number.trim();
        if (trimmed.length() <= 4) {
            return "•".repeat(trimmed.length());
        }
        return "•••• " + trimmed.substring(trimmed.length() - 4);
    }


    @org.springframework.transaction.annotation.Transactional
    public List<com.tripify.user_auth_service.dto.response.UserResponse> getAllOrganizers() {
        return userRepository.findByRole(Role.ROLE_ORGANIZER).stream()
                .map(u -> toPublicResponse(u, "Organizzatore", true))
                .toList();
    }

    @org.springframework.transaction.annotation.Transactional
    public com.tripify.user_auth_service.dto.response.UserResponse getOrganizerById(String identifier) {
        User u = findExistingUser(identifier)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        if (u.getRole() != Role.ROLE_ORGANIZER) {
            throw new ResourceNotFoundException("L'utente richiesto non è un organizzatore");
        }
        return toPublicResponse(u, "Organizzatore", true);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public com.tripify.user_auth_service.dto.response.UserResponse getUserSummary(String identifier) {
        User u = findExistingUser(identifier)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        return toPublicResponse(u, "Utente", false);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Map<String, String> resolveDisplayNames(Collection<String> subs) {
        if (subs == null || subs.isEmpty()) {
            return Map.of();
        }
        List<String> lookup = subs.stream()
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .limit(200)
                .toList();
        if (lookup.isEmpty()) {
            return Map.of();
        }
        Map<String, String> result = new java.util.HashMap<>();
        for (User u : userRepository.findByUsernameIn(lookup)) {
            if (u.getUsername() == null) {
                continue;
            }
            String name = u.getName() != null ? u.getName().trim() : "";
            String surname = u.getSurname() != null ? u.getSurname().trim() : "";
            String display = (name + " " + surname).trim();
            if (!display.isEmpty()) {
                result.put(u.getUsername(), display);
            }
        }
        return result;
    }

    private java.util.Optional<User> findExistingUser(String identifier) {
        if (identifier != null && identifier.contains("@")) {
            return userRepository.findByEmail(identifier);
        }
        return userRepository.findByUsername(identifier)
                .or(() -> {
                    try {
                        return userRepository.findById(UUID.fromString(identifier));
                    } catch (IllegalArgumentException notAUuid) {
                        return java.util.Optional.empty();
                    }
                });
    }

    private String resolveKeycloakSubject(User user) {
        if (user.getUsername() != null && !user.getUsername().isEmpty()) {
            return user.getUsername();
        }
        try {
            List<UserRepresentation> matches = getKeycloakAdminClient()
                    .realm("tripify").users().searchByEmail(user.getEmail(), true);
            if (!matches.isEmpty() && matches.get(0).getId() != null && !matches.get(0).getId().isBlank()) {
                user.setUsername(matches.get(0).getId());
                userRepository.save(user);
                return user.getUsername();
            }
        } catch (Exception e) {
            log.warn("Impossibile risolvere il subject Keycloak per {}: {}", user.getEmail(), e.getMessage());
        }
        return null;
    }

    private com.tripify.user_auth_service.dto.response.UserResponse toPublicResponse(User u, String fallbackName, boolean includeEmail) {
        return new com.tripify.user_auth_service.dto.response.UserResponse(
                resolveKeycloakSubject(u),
                u.getName() != null ? u.getName() : fallbackName,
                u.getSurname() != null ? u.getSurname() : "",
                includeEmail ? u.getEmail() : null,
                normalizeImageUrl(u.getProfilePictureUrl()),
                null,
                null,
                u.getCompanyName(),
                null,
                null,
                u.getRole() != null ? u.getRole().name() : null
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