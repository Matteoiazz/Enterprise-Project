package com.tripify.user_auth_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Estrai il token (togliendo i primi 7 caratteri "Bearer ")
        jwt = authHeader.substring(7);
        // 4. Estrai l'email dal token tramite il tuo JwtService
        userEmail = jwtService.extractUsername(jwt);

        // 5. Se l'email c'è e l'utente non è ancora autenticato nel contesto attuale
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Carica l'utente dal database
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // Verifica se il token è valido per quell'utente
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // Crea il "pass" per Spring Security
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Salva l'autenticazione. Da questo momento la rotta /api/v1/profile è accessibile!
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Passa la palla al prossimo filtro
        filterChain.doFilter(request, response);
    }
}