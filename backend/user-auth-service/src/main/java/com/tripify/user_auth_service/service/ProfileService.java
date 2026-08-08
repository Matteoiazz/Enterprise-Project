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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final CompanionRepository companionRepository;
    private final TravelDocumentRepository travelDocumentRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));
    }

    // --- COMPANIONS ---
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

    // --- TRAVEL DOCUMENTS ---
    public List<TravelDocumentDto> getTravelDocuments(String userEmail) {
        return travelDocumentRepository.findByUser(getUser(userEmail)).stream()
                .map(d -> TravelDocumentDto.builder().id(d.getId()).documentType(d.getDocumentType())
                        .documentNumber(d.getDocumentNumber()).expirationDate(d.getExpirationDate())
                        .issuingCountry(d.getIssuingCountry()).build())
                .collect(Collectors.toList());
    }

    public TravelDocumentDto addTravelDocument(String userEmail, TravelDocumentDto dto) {
        TravelDocument saved = travelDocumentRepository.save(TravelDocument.builder()
                .documentType(dto.getDocumentType()).documentNumber(dto.getDocumentNumber())
                .expirationDate(dto.getExpirationDate()).issuingCountry(dto.getIssuingCountry())
                .user(getUser(userEmail)).build());
        return TravelDocumentDto.builder().id(saved.getId()).documentType(saved.getDocumentType())
                .documentNumber(saved.getDocumentNumber()).expirationDate(saved.getExpirationDate())
                .issuingCountry(saved.getIssuingCountry()).build();
    }

    // --- PAYMENT METHODS ---
    public List<PaymentMethodDto> getPaymentMethods(String userEmail) {
        return paymentMethodRepository.findByUser(getUser(userEmail)).stream()
                .map(p -> PaymentMethodDto.builder().id(p.getId()).cardProvider(p.getCardProvider())
                        .lastFourDigits(p.getLastFourDigits()).expirationMonthYear(p.getExpirationMonthYear()).build())
                .collect(Collectors.toList());
    }

    public PaymentMethodDto addPaymentMethod(String userEmail, PaymentMethodDto dto) {
        // Estraggo solo le ultime 4 cifre per sicurezza
        String lastFour = (dto.getCardNumber() != null && dto.getCardNumber().length() >= 4)
                ? dto.getCardNumber().substring(dto.getCardNumber().length() - 4)
                : "0000";

        PaymentMethod saved = paymentMethodRepository.save(PaymentMethod.builder()
                .cardProvider(dto.getCardProvider()).lastFourDigits(lastFour)
                .expirationMonthYear(dto.getExpirationMonthYear()).user(getUser(userEmail)).build());

        return PaymentMethodDto.builder().id(saved.getId()).cardProvider(saved.getCardProvider())
                .lastFourDigits(saved.getLastFourDigits()).expirationMonthYear(saved.getExpirationMonthYear()).build();
    }
}