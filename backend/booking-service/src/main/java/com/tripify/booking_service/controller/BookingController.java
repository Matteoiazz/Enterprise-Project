package com.tripify.booking_service.controller;

import com.tripify.booking_service.dto.AuditLogEntryDTO;
import com.tripify.booking_service.dto.BookingResponseDTO;
import com.tripify.booking_service.dto.CheckoutRequestDTO;
import com.tripify.booking_service.dto.PassengerRequestDTO;
import com.tripify.booking_service.dto.ReceivedBookingLineDTO;
import com.tripify.booking_service.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // userId letto dal claim "sub" del JWT già verificato da Spring Security,
    // non più dall'header (vedi discussione sicurezza precedente). Body opzionale:
    // se assente o senza cartItemIds si fa il checkout dell'intero carrello,
    // altrimenti solo degli articoli selezionati (vedi BookingService.checkout).
    @PostMapping("/checkout")
    public ResponseEntity<BookingResponseDTO> checkoutCart(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody(required = false) CheckoutRequestDTO request) {
        String userId = jwt.getSubject();
        List<Long> selectedCartItemIds = request != null ? request.cartItemIds() : null;
        return ResponseEntity.ok(bookingService.checkout(userId, selectedCartItemIds));
    }

    @GetMapping("/user")
    public ResponseEntity<Page<BookingResponseDTO>> getUserBookings(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "bookingDate", direction = Sort.Direction.DESC) Pageable pageable) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(bookingService.getUserHistory(userId, pageable));
    }

    // Prenotazioni fatte da altri utenti sugli annunci pubblicati da chi chiama
    // (es. l'host di un hotel vuole vedere chi ha prenotato le sue camere).
    // L'identità di chi chiama non serve qui come parametro esplicito: viaggia
    // dentro il token JWT già inoltrato a catalog-service da FeignClientConfig.
    @GetMapping("/received")
    public ResponseEntity<List<ReceivedBookingLineDTO>> getReceivedBookings() {
        return ResponseEntity.ok(bookingService.getReceivedBookings());
    }

    // friendId resta un dato in ingresso legittimo (chi il chiamante autenticato
    // vuole invitare), non l'identità del chiamante: giusto lasciarlo come query param.
    @PostMapping("/{bookingId}/invite")
    public ResponseEntity<BookingResponseDTO> inviteToTrip(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long bookingId,
            @RequestParam String friendId) {

        String leaderId = jwt.getSubject();
        return ResponseEntity.ok(bookingService.inviteFriend(bookingId, leaderId, friendId));
    }

    // Annulla una prenotazione (solo il leader). Se era già pagata, avvia anche
    // il rimborso simulato tramite PaymentService (vedi BookingService.cancelBooking).
    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<BookingResponseDTO> cancelBooking(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long bookingId) {

        String requesterId = jwt.getSubject();
        return ResponseEntity.ok(bookingService.cancelBooking(bookingId, requesterId));
    }

    // Storico audit di una prenotazione. Visibile solo a leader/partecipanti,
    // il controllo di autorizzazione vive nel service (getAuditHistory).
    @GetMapping("/{bookingId}/audit")
    public ResponseEntity<List<AuditLogEntryDTO>> getAuditHistory(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long bookingId) {

        String requesterId = jwt.getSubject();
        return ResponseEntity.ok(bookingService.getAuditHistory(bookingId, requesterId));
    }

    // Aggiunge un passeggero (con documento già risolto/autocompilato lato
    // Android) a una specifica riga di prenotazione. Solo il leader può farlo.
    @PostMapping("/lines/{bookingLineId}/passengers")
    public ResponseEntity<Void> addPassenger(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long bookingLineId,
            @Valid @RequestBody PassengerRequestDTO request) {

        String requesterId = jwt.getSubject();
        bookingService.addPassenger(bookingLineId, requesterId, request);
        return ResponseEntity.ok().build();
    }
}
