package com.tripify.catalog_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Obbligatorio per testare le POST (es. inserire dati)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/catalog/**").permitAll() // LIBERA IL TUO CONTROLLER
                        .anyRequest().authenticated() // Tutto il resto rimane protetto
                );
        return http.build();
    }
}