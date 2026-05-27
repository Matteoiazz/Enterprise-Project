package com.tripify.booking_service.controller;

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

    private final BookingService bookingService; // SBLOCCATO

    @PostMapping("/checkout/{userId}")
    public ResponseEntity<Booking> checkoutCart(@PathVariable String userId) {
        return ResponseEntity.ok(bookingService.checkout(userId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Booking>> getUserBookings(@PathVariable String userId) {
        return ResponseEntity.ok(bookingService.getUserHistory(userId));
    }
}