package com.tripify.booking_service.dto;

import java.time.LocalDateTime;

// Rispecchia HoldResultDTO di catalog-service: risposta agli endpoint di hold.
public record HoldResultDTO(String holdId, LocalDateTime expiresAt) {}
