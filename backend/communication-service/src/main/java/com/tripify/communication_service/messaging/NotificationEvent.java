package com.tripify.communication_service.messaging;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private String userId;
    private String title;
    private String message;
}