package com.tripify.booking_service.controller;

import com.tripify.booking_service.dto.BookingResponseDTO;
import com.tripify.booking_service.entity.Booking;
import com.tripify.booking_service.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // PUNTO 3: Ora userId arriva dall'Header in modo sicuro
    @PostMapping("/checkout")
    public ResponseEntity<Booking> checkoutCart(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(bookingService.checkout(userId));
    }

    // PUNTO 3: Ora userId arriva dall'Header in modo sicuro
    @GetMapping("/user")
    public ResponseEntity<List<BookingResponseDTO>> getUserBookings(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(bookingService.getUserHistory(userId));
    }

    // PUNTO 1: Il nuovo endpoint per invitare gli amici!
    @PostMapping("/{bookingId}/invite")
    public ResponseEntity<BookingResponseDTO> inviteToTrip(
            @PathVariable Long bookingId,
            @RequestHeader("X-User-Id") String leaderId,
            @RequestParam String friendId) {

        return ResponseEntity.ok(bookingService.inviteFriend(bookingId, leaderId, friendId));
    }
}