package com.tripify.communication_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disabilita CSRF per testare da terminale/Android
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/chat/**", "/ws-chat/**").permitAll() // Sblocca le rotte chat
                        .anyRequest().authenticated() // Mantiene protetto il resto
                );
        return http.build();
    }
}