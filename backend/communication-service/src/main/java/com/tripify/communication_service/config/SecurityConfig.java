package com.tripify.communication_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .authorizeHttpRequests(auth -> auth
                        // Manteniamo le rotte WebSocket e Chat aperte, come richiesto dallo script
                        .requestMatchers("/chat/**", "/ws-chat/**").permitAll()
                        // Le notifiche (e tutto il resto) richiedono l'autenticazione
                        .anyRequest().authenticated()
                )
                // L'AGGIUNTA FONDAMENTALE: Abilita la validazione del Bearer Token JWT
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }
}