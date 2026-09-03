package com.tripify.user_auth_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequestDTO {

    @Size(min = 2, max = 50, message = "Il nome deve avere tra 2 e 50 caratteri")
    private String name;

    @Size(min = 2, max = 50, message = "Il cognome deve avere tra 2 e 50 caratteri")
    private String surname;

    @Pattern(regexp = "^(\\+|00)?[0-9][0-9\\s.\\-()/]{5,24}$",
            message = "Formato telefono non valido (es. +39 333 1234567)")
    private String phone;

    @Size(max = 100, message = "L'indirizzo è troppo lungo")
    private String address;

    @Size(min = 8, message = "La password deve contenere almeno 8 caratteri")
    private String newPassword;

    private String currentPassword;

    @Email(message = "L'email deve essere in un formato valido (es. mario@mail.com)")
    private String email;
}
