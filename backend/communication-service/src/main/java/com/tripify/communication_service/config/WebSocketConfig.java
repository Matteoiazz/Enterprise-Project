package com.tripify.communication_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Questo è l'URL a cui Android si connetterà all'inizio (es. ws://localhost:8084/ws-chat)
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*"); // Un fallback nel caso in cui i WebSockets non siano supportati
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // I messaggi inviati DAL server verso Android avranno questo prefisso
        registry.enableSimpleBroker("/topic", "/queue");

        // I messaggi inviati DA Android verso il server avranno questo prefisso
        registry.setApplicationDestinationPrefixes("/app");
    }
}