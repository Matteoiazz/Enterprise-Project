package com.tripify.user_auth_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentMethodDto {
    private UUID id;
    private String cardProvider;
    private String cardNumber;
    private String lastFourDigits;
    private String expirationMonthYear;
}