package com.tripify.booking_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // API stateless: nessuna sessione HTTP, l'identità viaggia nel JWT ad ogni richiesta
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // CSRF non serve per API stateless senza cookie di sessione
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Se in futuro aggiungete Swagger, andrà eslicitato qui come permitAll
                        .anyRequest().authenticated()
                )
                // Attiva la validazione del JWT: Spring scarica automaticamente le chiavi
                // pubbliche dall'issuer-uri configurato in application.properties
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));

        return http.build();
    }
}