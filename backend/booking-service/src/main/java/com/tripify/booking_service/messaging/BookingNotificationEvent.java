package com.tripify.booking_service.messaging;

// Rispecchia NotificationEvent di communication-service: stessi nomi di campo
// (userId, title, message) così Jackson lo serializza/deserializza in modo
// compatibile sulla coda "notification_queue". In origine userId era Long lì
// mentre qui è sempre stato una UUID stringa (claim JWT "sub"); communication-
// service ha allineato il tipo a String, quindi ora la pubblicazione è attiva
// (vedi BookingEventPublisher).
public record BookingNotificationEvent(
        String userId,
        String title,
        String message
) {}
