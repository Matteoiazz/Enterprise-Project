package com.tripify.catalog_service.controller;

import com.tripify.catalog_service.dto.AvailabilityDTO;
import com.tripify.catalog_service.dto.HoldResultDTO;
import com.tripify.catalog_service.dto.RoomHoldRequestDTO;
import com.tripify.catalog_service.dto.SeatHoldRequestDTO;
import com.tripify.catalog_service.service.AvailabilityService;
import com.tripify.catalog_service.util.InternalKeyValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;


@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @Value("${internal.service-key}")
    private String internalServiceKey;

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
    public ResponseEntity<HoldResultDTO> holdRoom(@PathVariable Long id, @Valid @RequestBody RoomHoldRequestDTO request,
                                                   @AuthenticationPrincipal Jwt jwt) {
        HoldResultDTO result = availabilityService.holdRoom(id, request.checkIn(), request.checkOut(), request.rooms(), jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/fare-classes/{id}/hold")
    public ResponseEntity<HoldResultDTO> holdSeats(@PathVariable Long id, @Valid @RequestBody SeatHoldRequestDTO request,
                                                    @AuthenticationPrincipal Jwt jwt) {
        HoldResultDTO result = availabilityService.holdSeats(id, request.seats(), jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/holds/{holdId}/confirm")
    public ResponseEntity<Void> confirm(@PathVariable String holdId, @AuthenticationPrincipal Jwt jwt) {
        availabilityService.confirm(holdId, jwt.getSubject());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/holds/{holdId}/release")
    public ResponseEntity<Void> release(@PathVariable String holdId, @AuthenticationPrincipal Jwt jwt) {
        availabilityService.release(holdId, jwt.getSubject());
        return ResponseEntity.ok().build();
    }

    /**
     * Compensazione per booking-service: rilascia un hold anche se già CONFIRMED, per
     * quando un'altra parte della transazione (altro hold, pagamento) fallisce dopo
     * averlo confermato. Non è un'azione utente: niente JWT, protetto dalla chiave di
     * servizio (stesso meccanismo di CatalogController.updateRating).
     */
    @PostMapping("/holds/{holdId}/compensate")
    public ResponseEntity<Void> compensate(@PathVariable String holdId,
                                            @RequestHeader("X-Internal-Key") String internalKey) {
        if (!InternalKeyValidator.matches(internalServiceKey, internalKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        availabilityService.compensate(holdId);
        return ResponseEntity.ok().build();
    }
}
