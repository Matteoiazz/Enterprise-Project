package com.tripify.booking_service.service;

import com.tripify.booking_service.entity.Booking;
import com.tripify.booking_service.entity.Passenger;
import com.tripify.booking_service.messaging.BookingNotificationEvent;
import com.tripify.booking_service.repository.PassengerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// Job schedulato: apre il check-in generando il QR (un UUID opaco, verificato
// via lookup sul server - non serve firmarlo) per i passeggeri di prenotazioni
// CONFERMATE la cui riga ha check-in entro domani (hotel), o subito per le
// righe senza una data propria (voli/attività - vedi PassengerRepository).
// qrCodeData nullo/non nullo è già di per sé il flag "già processato": non
// serve un'altra colonna per evitare di rigenerarlo o rinotificare più volte.
@Service
@RequiredArgsConstructor
@Slf4j
public class CheckInService {

    private final PassengerRepository passengerRepository;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void openCheckIn() {
        LocalDate threshold = LocalDate.now().plusDays(1);
        List<Passenger> eligible = passengerRepository.findEligibleForCheckIn(threshold);
        if (eligible.isEmpty()) {
            return;
        }

        for (Passenger passenger : eligible) {
            passenger.setQrCodeData(UUID.randomUUID().toString());
        }
        passengerRepository.saveAll(eligible);

        // Un solo avviso per prenotazione anche se più passeggeri diventano
        // idonei insieme: .distinct() si appoggia al fatto che, nella stessa
        // transazione, Hibernate restituisce sempre la stessa istanza Java
        // per la stessa riga (first-level cache), quindi l'uguaglianza per
        // riferimento basta a deduplicare senza bisogno di equals/hashCode.
        eligible.stream()
                .map(passenger -> passenger.getBookingLine().getBooking())
                .distinct()
                .forEach(booking -> sendCheckInOpenNotification(booking));

        log.info("Check-in aperto per {} passeggeri", eligible.size());
    }

    private void sendCheckInOpenNotification(Booking booking) {
        try {
            rabbitTemplate.convertAndSend("notification_queue", new BookingNotificationEvent(
                    booking.getUserId(),
                    "Check-in aperto ✈️",
                    "Il check-in per la tua prenotazione è ora disponibile: mostra il QR in-app all'ingresso."
            ));
        } catch (Exception e) {
            log.warn("Impossibile inviare la notifica di check-in aperto per la prenotazione {}", booking.getId(), e);
        }
    }
}
