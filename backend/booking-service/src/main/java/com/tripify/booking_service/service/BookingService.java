package com.tripify.booking_service.service;

import com.tripify.booking_service.dto.BookingResponseDTO;
import com.tripify.booking_service.entity.*;
import com.tripify.booking_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    // 2. AGGIORNATO: Recupera lo storico calcolando i permessi Leader vs Partecipante
    public List<BookingResponseDTO> getUserHistory(String userId) {

        // Pesca i viaggi dove l'utente è leader o è stato invitato tra i partecipanti
        List<Booking> bookings = bookingRepository.findAllByUserIdOrParticipantIdsContaining(userId, userId);

        // Mappiamo le entità del database nei DTO per Android
        return bookings.stream().map(booking -> {

            // Logica magica: sei il leader solo se il tuo ID coincide con quello di chi ha prenotato
            boolean isLeader = booking.getUserId().equals(userId);

            return BookingResponseDTO.builder()
                    .id(booking.getId())
                    .totalAmount(booking.getTotalAmount())
                    .bookingDate(booking.getBookingDate())
                    .status(booking.getStatus())
                    .isLeader(isLeader) // Passiamo il flag al frontend
                    .build();

        }).collect(Collectors.toList());
    }

    // 3. IL METODO MANCANTE: Permette al Leader di invitare gli amici
    @Transactional
    public BookingResponseDTO inviteFriend(Long bookingId, String leaderId, String friendId) {
        // Cerca il viaggio
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Prenotazione non trovata!"));

        // Controllo di sicurezza: solo chi ha pagato (il leader) può invitare
        if (!booking.getUserId().equals(leaderId)) {
            throw new RuntimeException("Accesso negato: solo il creatore del viaggio può invitare amici.");
        }

        // Aggiunge l'amico alla lista e salva nel database
        booking.getParticipantIds().add(friendId);
        bookingRepository.save(booking);

        // Restituisce il viaggio aggiornato (con isLeader = true, dato che l'ha chiamato il leader)
        return BookingResponseDTO.builder()
                .id(booking.getId())
                .totalAmount(booking.getTotalAmount())
                .bookingDate(booking.getBookingDate())
                .status(booking.getStatus())
                .isLeader(true)
                .build();
    }
}