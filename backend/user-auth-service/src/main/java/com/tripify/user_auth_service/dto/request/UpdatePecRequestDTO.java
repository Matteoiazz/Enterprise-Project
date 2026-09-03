package com.tripify.user_auth_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePecRequestDTO {

    @NotBlank(message = "La PEC è obbligatoria")
    @Email(message = "La PEC deve essere un indirizzo email valido")
    @Size(max = 255, message = "La PEC è troppo lunga")
    private String pec;
}
