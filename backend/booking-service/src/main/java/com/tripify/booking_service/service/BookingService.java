package com.tripify.booking_service.service;

import com.tripify.booking_service.dto.AuditLogEntryDTO;
import com.tripify.booking_service.dto.BookingResponseDTO;
import com.tripify.booking_service.dto.PassengerRequestDTO;
import com.tripify.booking_service.entity.*;
import com.tripify.booking_service.exception.AccessDeniedException;
import com.tripify.booking_service.exception.ResourceNotFoundException;
import com.tripify.booking_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingLineRepository bookingLineRepository;
    private final PassengerRepository passengerRepository;
    private final ShoppingCartService cartService;
    private final BookingAuditService auditService;

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

        // Calcoliamo il totale complessivo degli elementi nel carrello (BigDecimal, niente errori di arrotondamento)
        BigDecimal totalAmount = cart.getItems().stream()
                .map(item -> item.getPriceAtAdded().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Creiamo la testata della prenotazione (inizialmente in stato PENDING)
        // createdAt/updatedAt li scrive Hibernate da solo (@CreationTimestamp/@UpdateTimestamp)
        Booking booking = Booking.builder()
                .userId(userId)
                .totalAmount(totalAmount)
                .bookingDate(LocalDateTime.now())
                .status(BookingStatus.PENDING)
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

            // I passeggeri (con relativo documento) si aggiungono dopo il checkout
            // tramite addPassenger(), non qui: al momento del checkout sappiamo
            // solo cosa è stato acquistato, non ancora chi viaggerà su ogni riga.
            lines.add(line);
        }

        savedBooking.setLines(lines);
        bookingRepository.save(savedBooking);

        // Evento di audit: creazione della prenotazione. Stessa transazione del
        // salvataggio sopra, quindi se qualcosa fallisce dopo, anche il log
        // viene annullato insieme al resto (nessun log "orfano" di eventi mai accaduti).
        auditService.log(savedBooking, userId, AuditAction.CREATED,
                "Prenotazione creata con " + lines.size() + " elemento/i, totale " + totalAmount + "€");

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

    // 3. Permette al Leader di invitare gli amici
    // updatedAt si aggiorna da solo grazie a @UpdateTimestamp, non serve più gestirlo qui.
    @Transactional
    public BookingResponseDTO inviteFriend(Long bookingId, String leaderId, String friendId) {
        // Cerca il viaggio -> se non esiste, 404 grazie a ResourceNotFoundException
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Prenotazione non trovata!"));

        // Controllo di sicurezza: solo chi ha pagato (il leader) può invitare -> 403 se fallisce
        if (!booking.getUserId().equals(leaderId)) {
            throw new AccessDeniedException("Accesso negato: solo il creatore del viaggio può invitare amici.");
        }

        // Aggiunge l'amico alla lista e salva nel database
        booking.getParticipantIds().add(friendId);
        bookingRepository.save(booking);

        // Evento di audit: partecipante aggiunto. performedBy è il leader che ha
        // eseguito l'azione, non il friendId invitato (quello va nel dettaglio).
        auditService.log(booking, leaderId, AuditAction.PARTICIPANT_ADDED,
                "Invitato partecipante con id " + friendId);

        // Restituisce il viaggio aggiornato (con isLeader = true, dato che l'ha chiamato il leader)
        return BookingResponseDTO.builder()
                .id(booking.getId())
                .totalAmount(booking.getTotalAmount())
                .bookingDate(booking.getBookingDate())
                .status(booking.getStatus())
                .isLeader(true)
                .build();
    }

    // 4. Storico eventi di una prenotazione.
    // Autorizzazione: può vederlo solo il leader o uno dei partecipanti invitati.
    public List<AuditLogEntryDTO> getAuditHistory(Long bookingId, String requesterId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Prenotazione non trovata!"));

        boolean isLeader = booking.getUserId().equals(requesterId);
        boolean isParticipant = booking.getParticipantIds().contains(requesterId);

        if (!isLeader && !isParticipant) {
            throw new AccessDeniedException("Accesso negato: non fai parte di questa prenotazione.");
        }

        return auditService.getHistory(bookingId).stream()
                .map(entry -> new AuditLogEntryDTO(
                        entry.getAction(),
                        entry.getPerformedBy(),
                        entry.getDetails(),
                        entry.getCreatedAt()))
                .collect(Collectors.toList());
    }

    // 5. NUOVO: Associa un passeggero (con documento congelato) a una riga di prenotazione.
    // I dati arrivano già risolti da Android (autocompilati leggendo da user-auth-service,
    // o inseriti a mano) - qui li salviamo così come sono, senza richiamare altri servizi.
    // Autorizzazione: solo il leader della Booking a cui appartiene la riga può farlo,
    // stesso criterio già usato per inviteFriend().
    @Transactional
    public void addPassenger(Long bookingLineId, String requesterId, PassengerRequestDTO request) {
        BookingLine line = bookingLineRepository.findById(bookingLineId)
                .orElseThrow(() -> new ResourceNotFoundException("Riga di prenotazione non trovata!"));

        Booking booking = line.getBooking();
        if (!booking.getUserId().equals(requesterId)) {
            throw new AccessDeniedException("Accesso negato: solo il creatore del viaggio può aggiungere passeggeri.");
        }

        Passenger passenger = Passenger.builder()
                .bookingLine(line)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .taxCode(request.taxCode())
                .documentType(request.documentType())
                .documentNumber(request.documentNumber())
                .documentExpirationDate(request.documentExpirationDate())
                .issuingCountry(request.issuingCountry())
                .checkedIn(false)
                .build();

        passengerRepository.save(passenger);

        auditService.log(booking, requesterId, AuditAction.PASSENGER_ADDED,
                "Aggiunto passeggero " + request.firstName() + " " + request.lastName()
                        + " alla riga " + bookingLineId);
    }
}