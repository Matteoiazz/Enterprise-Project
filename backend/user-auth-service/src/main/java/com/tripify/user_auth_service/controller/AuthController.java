package com.tripify.user_auth_service.controller;

import com.tripify.user_auth_service.dto.request.LoginRequest;
import com.tripify.user_auth_service.dto.request.RegisterRequest;
import com.tripify.user_auth_service.dto.response.AuthResponse;

import com.tripify.user_auth_service.dto.response.UserResponse;
import com.tripify.user_auth_service.entity.User;
import com.tripify.user_auth_service.repository.UserRepository;
import com.tripify.user_auth_service.security.JwtService;
import com.tripify.user_auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String token) {
        String jwt = token.startsWith("Bearer ") ? token.substring(7) : token;

        String email = jwtService.extractUsername(jwt);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        // 4. Mappa i dati nel DTO di risposta
        UserResponse response = new UserResponse(
                user.getName(),
                user.getSurname(),
                user.getEmail()
        );

        return ResponseEntity.ok(response);
    }
}