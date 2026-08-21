package com.tripify.booking_service.controller;

import com.tripify.booking_service.dto.AuditLogEntryDTO;
import com.tripify.booking_service.dto.BookingResponseDTO;
import com.tripify.booking_service.dto.PassengerRequestDTO;
import com.tripify.booking_service.entity.Booking;
import com.tripify.booking_service.service.BookingService;
import lombok.RequiredArgsConstructor;
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
    // non più dall'header (vedi discussione sicurezza precedente).
    @PostMapping("/checkout")
    public ResponseEntity<Booking> checkoutCart(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(bookingService.checkout(userId));
    }

    @GetMapping("/user")
    public ResponseEntity<List<BookingResponseDTO>> getUserBookings(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(bookingService.getUserHistory(userId));
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

    // Storico audit di una prenotazione. Visibile solo a leader/partecipanti,
    // il controllo di autorizzazione vive nel service (getAuditHistory).
    @GetMapping("/{bookingId}/audit")
    public ResponseEntity<List<AuditLogEntryDTO>> getAuditHistory(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long bookingId) {

        String requesterId = jwt.getSubject();
        return ResponseEntity.ok(bookingService.getAuditHistory(bookingId, requesterId));
    }

    // NUOVO: aggiunge un passeggero (con documento già risolto/autocompilato lato
    // Android) a una specifica riga di prenotazione. Solo il leader può farlo.
    @PostMapping("/lines/{bookingLineId}/passengers")
    public ResponseEntity<Void> addPassenger(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long bookingLineId,
            @RequestBody PassengerRequestDTO request) {

        String requesterId = jwt.getSubject();
        bookingService.addPassenger(bookingLineId, requesterId, request);
        return ResponseEntity.ok().build();
    }
}