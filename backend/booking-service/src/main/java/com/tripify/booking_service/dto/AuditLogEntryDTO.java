package com.tripify.booking_service.dto;

import com.tripify.booking_service.entity.AuditAction;

import java.time.LocalDateTime;

public record AuditLogEntryDTO(
        AuditAction action,
        String performedBy,
        String details,
        LocalDateTime createdAt
) {}