package com.tripify.booking_service.messaging;

import com.tripify.booking_service.entity.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

// Predisposto per pubblicare eventi verso la stessa coda RabbitMQ
// ("notification_queue") che communication-service ascolta già
// (vedi communication-service NotificationConsumer), ma NON è collegato a
// nessuna chiamata reale in BookingService/PaymentService: il mismatch di
// tipo sullo userId (vedi BookingNotificationEvent) va risolto lato
// communication-service prima di attivare l'invio.
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventPublisher {

    private static final String NOTIFICATION_QUEUE = "notification_queue";

    private final RabbitTemplate rabbitTemplate;

    public void publishBookingConfirmed(Booking booking) {
        BookingNotificationEvent event = new BookingNotificationEvent(
                booking.getUserId(),
                "Prenotazione confermata",
                "La tua prenotazione #" + booking.getId() + " è stata confermata."
        );
        log.info("Evento di notifica pronto per la prenotazione {} ma non pubblicato (vedi nota su BookingNotificationEvent)", booking.getId());
        // rabbitTemplate.convertAndSend(NOTIFICATION_QUEUE, event);
    }
}
