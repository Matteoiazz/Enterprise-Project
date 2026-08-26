package com.tripify.booking_service.messaging;

import com.tripify.booking_service.entity.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

// Pubblica eventi sulla stessa coda RabbitMQ ("notification_queue") che
// communication-service ascolta (vedi communication-service NotificationConsumer).
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
        try {
            rabbitTemplate.convertAndSend(NOTIFICATION_QUEUE, event);
        } catch (RuntimeException ex) {
            // Una notifica mancata non deve mai far fallire il pagamento/la
            // prenotazione già andata a buon fine: logghiamo e proseguiamo.
            log.warn("Impossibile pubblicare la notifica per la prenotazione {}: {}", booking.getId(), ex.getMessage());
        }
    }
}
