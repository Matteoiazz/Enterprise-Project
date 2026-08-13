package com.tripify.user_auth_service.service;

import com.tripify.user_auth_service.dto.request.CompanionDto;
import com.tripify.user_auth_service.dto.request.PaymentMethodDto;
import com.tripify.user_auth_service.dto.request.TravelDocumentDto;
import com.tripify.user_auth_service.entity.Companion;
import com.tripify.user_auth_service.entity.PaymentMethod;
import com.tripify.user_auth_service.entity.TravelDocument;
import com.tripify.user_auth_service.entity.User;
import com.tripify.user_auth_service.repository.CompanionRepository;
import com.tripify.user_auth_service.repository.PaymentMethodRepository;
import com.tripify.user_auth_service.repository.TravelDocumentRepository;
import com.tripify.user_auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final CompanionRepository companionRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final TravelDocumentRepository documentRepository;

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .password("MANAGED_BY_KEYCLOAK")
                            .role(com.tripify.user_auth_service.entity.Role.ROLE_TRAVELER)
                            .build();

                    return userRepository.save(newUser);
                });
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
}