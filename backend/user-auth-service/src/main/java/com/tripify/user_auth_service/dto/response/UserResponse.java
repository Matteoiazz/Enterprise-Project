package com.tripify.user_auth_service.dto.response;

public record UserResponse(
        String id,
        String name,
        String surname,
        String email,
        String profilePictureUrl,
        String phone,
        String address,
        String companyName,
        String vatNumber,
        String pec,
        String role
) {
}
