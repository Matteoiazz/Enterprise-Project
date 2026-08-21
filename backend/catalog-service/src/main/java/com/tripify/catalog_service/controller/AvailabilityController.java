package com.tripify.catalog_service.controller;

import com.tripify.catalog_service.dto.AvailabilityDTO;
import com.tripify.catalog_service.dto.HoldResultDTO;
import com.tripify.catalog_service.dto.RoomHoldRequestDTO;
import com.tripify.catalog_service.dto.SeatHoldRequestDTO;
import com.tripify.catalog_service.service.AvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;


@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping("/room-types/{id}/availability")
    public ResponseEntity<AvailabilityDTO> roomAvailability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut
    ) {
        return ResponseEntity.ok(new AvailabilityDTO(availabilityService.computeRoomAvailability(id, checkIn, checkOut)));
    }

    @GetMapping("/fare-classes/{id}/availability")
    public ResponseEntity<AvailabilityDTO> seatAvailability(@PathVariable Long id) {
        return ResponseEntity.ok(new AvailabilityDTO(availabilityService.computeSeatAvailability(id)));
    }

    @PostMapping("/room-types/{id}/hold")
    public ResponseEntity<HoldResultDTO> holdRoom(@PathVariable Long id, @Valid @RequestBody RoomHoldRequestDTO request) {
        HoldResultDTO result = availabilityService.holdRoom(id, request.checkIn(), request.checkOut(), request.rooms(), request.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/fare-classes/{id}/hold")
    public ResponseEntity<HoldResultDTO> holdSeats(@PathVariable Long id, @Valid @RequestBody SeatHoldRequestDTO request) {
        HoldResultDTO result = availabilityService.holdSeats(id, request.seats(), request.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/holds/{holdId}/confirm")
    public ResponseEntity<Void> confirm(@PathVariable String holdId) {
        availabilityService.confirm(holdId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/holds/{holdId}/release")
    public ResponseEntity<Void> release(@PathVariable String holdId) {
        availabilityService.release(holdId);
        return ResponseEntity.ok().build();
    }
}
