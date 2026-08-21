package com.tripify.catalog_service.dto;

import java.time.LocalDateTime;

public record HoldResultDTO(String holdId, LocalDateTime expiresAt) {
}
