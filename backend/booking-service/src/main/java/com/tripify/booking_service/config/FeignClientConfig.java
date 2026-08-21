package com.tripify.booking_service.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// Senza questo, le chiamate Feign verso altri microservizi (es. UserAuthClient)
// partirebbero SENZA il token JWT dell'utente originale, e user-auth-service
// risponderebbe 401 non sapendo chi sta chiedendo i dati.
//
// Qui prendiamo l'header "Authorization" della richiesta HTTP in corso
// (quella che sta arrivando al booking-service) e lo ripropaghiamo identico
// sulla richiesta Feign in uscita, così l'identità dell'utente "attraversa"
// entrambi i servizi con lo stesso token.
@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor bearerTokenForwardingInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                String authorizationHeader = attributes.getRequest().getHeader("Authorization");
                if (authorizationHeader != null) {
                    requestTemplate.header("Authorization", authorizationHeader);
                }
            }
        };
    }
}