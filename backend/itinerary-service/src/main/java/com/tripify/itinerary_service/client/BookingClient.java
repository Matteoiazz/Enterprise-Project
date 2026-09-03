package com.tripify.itinerary_service.client;

import com.tripify.itinerary_service.dto.AddToCartRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

// booking-service legge l'utente solo dal JWT (jwt.getSubject()), non da un header
// id: il token dell'utente va quindi inoltrato così com'è, non sostituito da una
// identità di servizio (vedi ItineraryService.bookAllItems).
@FeignClient(name = "booking-service", url = "${booking-service.url}")
public interface BookingClient {

    @PostMapping("/api/v1/cart/add")
    void addToCart(@RequestHeader("Authorization") String authorizationHeader,
                   @RequestBody AddToCartRequestDTO request);
}
