package com.tripify.communication_service.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String NOTIFICATION_QUEUE = "notification_queue";

    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        // Usiamo il costruttore vuoto della nuova libreria
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();

        // Mappatura esplicita per ignorare il TypeId del mittente
        DefaultClassMapper classMapper = new DefaultClassMapper();
        classMapper.setTrustedPackages("*"); // Accettiamo pacchetti da altri servizi

        // Diciamo a RabbitMQ: "Qualsiasi cosa arrivi, prova a convertirla nel nostro NotificationEvent"
        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put("com.tripify.booking_service.messaging.BookingNotificationEvent",
                com.tripify.communication_service.messaging.NotificationEvent.class);
        idClassMapping.put("com.tripify.user_auth_service.messaging.NotificationEvent",
                com.tripify.communication_service.messaging.NotificationEvent.class);

        classMapper.setIdClassMapping(idClassMapping);
        converter.setClassMapper(classMapper);

        return converter;
    }
}