package com.tripify.catalog_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Leggiamo gli header che il Gateway ci ha gentilmente preparato
        String userId = request.getHeader("X-User-Id");
        String rolesHeader = request.getHeader("X-User-Roles"); // Es: "[ROLE_ORGANIZER]"

        // Se ci sono, l'utente è stato validato dal Gateway
        if (userId != null && rolesHeader != null) {

            // Puliamo la stringa dai caratteri speciali come [ ] e "
            String cleanRoles = rolesHeader.replace("[", "").replace("]", "").replace("\"", "");

            // Creiamo i ruoli per Spring Security
            List<SimpleGrantedAuthority> authorities = Arrays.stream(cleanRoles.split(","))
                    .map(String::trim)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            // Diciamo a Spring Security che l'utente è loggato e ha questi ruoli
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}