package com.travelapp.user_auth_service.dto.request;

import com.travelapp.user_auth_service.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank( message = "Email obbligatoria")
    @Email( message = "Email non valida")
    private String email;

    @NotBlank( message = "Password obbligatoria")
    @Size(min = 6, message = "La password deve essere di almeno 6 caratteri")
    private String password;

    @NotNull( message = "Ruolo obbligatorio")
    private Role role;

    private String name;
    private String surname;
    private String username;
    private String phone;
    private String address;
}

