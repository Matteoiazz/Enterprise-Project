package com.tripify.communication_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private Long id;
    private String userId;
    private String title;
    private String message;

    @JsonProperty("isRead")
    private boolean isRead;

    private LocalDateTime createdAt;
}