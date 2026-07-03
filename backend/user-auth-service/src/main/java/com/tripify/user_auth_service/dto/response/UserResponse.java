package com.tripify.user_auth_service.dto.response;

public record UserResponse(
        String name,
        String surname,
        String email
) {
}