package com.tripify.catalog_service.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Permettiamo a tutti di leggere il catalogo
                        .requestMatchers(HttpMethod.GET, "/api/v1/catalog/**").permitAll()

                        // SOLO gli ORGANIZER possono fare le POST per inserire voli e hotel
                        .requestMatchers(HttpMethod.POST, "/api/v1/catalog/items/**").hasAuthority("ROLE_ORGANIZER")

                        // Permettiamo Swagger
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()

                        // Qualsiasi altra richiesta deve essere autenticata
                        .anyRequest().authenticated()
                )
                // Aggiungiamo il nostro filtro custom PRIMA di quello standard di Spring
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable());

        return http.build();
    }
}