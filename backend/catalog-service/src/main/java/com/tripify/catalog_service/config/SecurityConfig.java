package com.tripify.catalog_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new JwtRoleConverter());

        http
                .csrf(csrf -> csrf.disable())
                .cors(withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/catalog/items/mine").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/catalog/**").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/v1/catalog/items/flights/**",
                                "/api/v1/catalog/items/hotels/**", "/api/v1/catalog/items/activities/**")
                                .hasAuthority("ROLE_ORGANIZER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/catalog/items/*/images").hasAuthority("ROLE_ORGANIZER")
                        // Non è un'azione utente: la protezione è la chiave di servizio controllata
                        // nel controller (vedi CatalogController.updateRating), non un JWT.
                        .requestMatchers(HttpMethod.PUT, "/api/v1/catalog/items/*/rating").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/catalog/items/**").hasAuthority("ROLE_ORGANIZER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/catalog/items/**").hasAuthority("ROLE_ORGANIZER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/catalog/items/**").hasAuthority("ROLE_ORGANIZER")

                        // Compensazione: chiamata di servizio da booking-service, non un utente
                        // (vedi AvailabilityController.compensate), protetta dalla chiave interna.
                        .requestMatchers(HttpMethod.POST, "/api/v1/catalog/holds/*/compensate").permitAll()
                        // Hold/confirm/release richiedono un utente reale autenticato: l'id viene letto dal JWT (sub).
                        .requestMatchers(HttpMethod.POST, "/api/v1/catalog/room-types/**", "/api/v1/catalog/fare-classes/**", "/api/v1/catalog/holds/**").authenticated()

                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()

                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable());

        return http.build();
    }
}