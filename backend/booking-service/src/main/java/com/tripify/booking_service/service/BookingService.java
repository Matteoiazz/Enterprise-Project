package com.tripify.booking_service.service;

import com.tripify.booking_service.entity.*;
import com.tripify.booking_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ShoppingCartService cartService;

    // Iniettiamo il client Feign o i dati per popolare le righe
    // (In futuro qui chiamerai Matteo per i dettagli del volo)

    // 1. Processo di Checkout: converte il carrello in una Booking confermata
    @Transactional
    public Booking checkout(String userId) {
        // Recuperiamo il carrello dell'utente
        ShoppingCart cart = cartService.getCartForUser(userId);

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Impossibile fare il checkout: il carrello è vuoto!");
        }

        // Calcoliamo il totale complessivo degli elementi nel carrello
        double totalAmount = cart.getItems().stream()
                .mapToDouble(item -> item.getPriceAtAdded() * item.getQuantity())
                .sum();

        // Creiamo la testata della prenotazione (inizialmente in stato PENDING)
        Booking booking = Booking.builder()
                .userId(userId)
                .totalAmount(totalAmount)
                .bookingDate(LocalDateTime.now())
                .status(BookingStatus.PENDING)
                .updatedAt(LocalDateTime.now())
                .lines(new ArrayList<>())
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        // Trasformiamo i CartItem in BookingLine
        List<BookingLine> lines = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            BookingLine line = BookingLine.builder()
                    .booking(savedBooking)
                    .catalogItemId(item.getCatalogItemId())
                    .price(item.getPriceAtAdded())
                    .passengers(new ArrayList<>())
                    .build();

            // Nota: Qui volendo si possono aggiungere dei passeggeri di default
            lines.add(line);
        }

        savedBooking.setLines(lines);
        bookingRepository.save(savedBooking);

        // Checkout andato a buon fine -> Svuotiamo il carrello dell'utente
        cartService.clearCart(userId);

        return savedBooking;
    }

    // 2. Recupera lo storico dei viaggi per l'app Android
    public List<Booking> getUserHistory(String userId) {
        return bookingRepository.findByUserIdOrderByBookingDateDesc(userId);
    }
}