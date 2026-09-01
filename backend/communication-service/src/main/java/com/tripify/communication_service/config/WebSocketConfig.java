package com.tripify.communication_service.config;

import com.tripify.communication_service.entity.ChatRoom;
import com.tripify.communication_service.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.time.Duration;
import java.util.Optional;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ChatRoomRepository chatRoomRepository;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Bean
    public JwtDecoder jwtDecoder() {
        org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(3000);

        RestTemplate restTemplate = new RestTemplate(requestFactory);

        String jwkSetUri = issuerUri + "/protocol/openid-connect/certs";

        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .restOperations(restTemplate)
                .build();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("http://localhost*", "https://localhost*", "app://*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor == null) {
                    return message;
                }

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");

                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        try {
                            Jwt jwt = jwtDecoder().decode(token);
                            JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
                            accessor.setUser(authentication);
                            log.info("WebSocket autenticata per l'utente: {}", jwt.getSubject());
                        } catch (Exception e) {
                            log.error("Token JWT non valido durante l'handshake WebSocket", e);
                            throw new IllegalArgumentException("Token non valido");
                        }
                    } else {
                        log.warn("Tentativo di connessione WebSocket senza token Bearer");
                        throw new IllegalArgumentException("Autenticazione richiesta");
                    }
                } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    String destination = accessor.getDestination();
                    Principal principal = accessor.getUser();

                    if (destination != null && principal != null) {
                        String userId = extractUserId(principal);

                        if (destination.startsWith("/topic/room/")) {
                            String roomId = destination.substring("/topic/room/".length());

                            Optional<ChatRoom> roomOpt = chatRoomRepository.findById(roomId);
                            if (roomOpt.isEmpty()) {
                                log.warn("Tentativo di subscribe a stanza inesistente: {}", roomId);
                                throw new IllegalArgumentException("Stanza non trovata");
                            }

                            ChatRoom room = roomOpt.get();
                            if (!userId.equals(room.getTravelerId()) && !userId.equals(room.getHostId())) {
                                log.error("ACCESSO NEGATO: L'utente {} ha tentato di sottoscriversi alla chat altrui {}", userId, roomId);
                                throw new IllegalArgumentException("Non sei autorizzato a leggere questa chat");
                            }
                            log.info("Accesso consentito alla chat {} per l'utente {}", roomId, userId);
                        } else if (destination.startsWith("/topic/notifications/")) {
                            String targetUserId = destination.substring("/topic/notifications/".length());

                            if (!userId.equals(targetUserId)) {
                                log.error("ACCESSO NEGATO: L'utente {} ha tentato di ascoltare le notifiche di {}", userId, targetUserId);
                                throw new IllegalArgumentException("Non sei autorizzato a leggere queste notifiche");
                            }
                            log.info("Accesso consentito al canale notifiche per l'utente {}", userId);
                        }
                    }
                }

                return message;
            }
        });
    }

    private String extractUserId(Principal principal) {
        if (principal instanceof JwtAuthenticationToken jwtToken) {
            return jwtToken.getToken().getSubject();
        }
        return principal.getName();
    }
}