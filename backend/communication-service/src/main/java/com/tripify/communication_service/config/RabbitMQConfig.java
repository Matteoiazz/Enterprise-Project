package com.tripify.communication_service.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Il nome della coda su cui saremo in ascolto
    public static final String NOTIFICATION_QUEUE = "notification_queue";

    @Bean
    public Queue notificationQueue() {
        // Crea la coda (true = la coda sopravvive ai riavvii di RabbitMQ)
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        // Converte in automatico il JSON in NotificationEvent
        return new Jackson2JsonMessageConverter();
    }
}