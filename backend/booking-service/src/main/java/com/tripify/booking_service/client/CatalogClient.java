package com.tripify.booking_service.client;

import com.tripify.booking_service.dto.CatalogItemSummaryDTO;
import com.tripify.booking_service.dto.HoldResultDTO;
import com.tripify.booking_service.dto.RoomHoldRequestDTO;
import com.tripify.booking_service.dto.SeatHoldRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// L'URL non è più scritto qui a mano: viene letto da application.properties
// (chiave catalog-service.url), così basta cambiare quella riga quando si passa
// a Docker, senza toccare questo file.
@FeignClient(name = "catalog-service", url = "${catalog-service.url}")
public interface CatalogClient {

    // Prezzo REALE dell'articolo: a differenza del vecchio /{itemId}/price (solo
    // prezzo base), qui arrivano anche roomTypes/fareClasses così ShoppingCartService
    // può usare il prezzo della camera/tariffa scelta invece del prezzo base.
    @GetMapping("/items/{itemId}")
    CatalogItemSummaryDTO getItem(@PathVariable("itemId") Long itemId);

    // Blocca temporaneamente delle camere su un RoomType (AvailabilityController
    // di catalog-service) per il tempo di completare carrello/checkout/pagamento.
    @PostMapping("/room-types/{id}/hold")
    HoldResultDTO holdRoom(@PathVariable("id") Long roomTypeId, @RequestBody RoomHoldRequestDTO request);

    // Blocca temporaneamente dei posti su un FareClass.
    @PostMapping("/fare-classes/{id}/hold")
    HoldResultDTO holdSeats(@PathVariable("id") Long fareClassId, @RequestBody SeatHoldRequestDTO request);

    // Trasforma un hold temporaneo in definitivo (pagamento riuscito).
    @PostMapping("/holds/{holdId}/confirm")
    void confirmHold(@PathVariable("holdId") String holdId);

    // Rilascia un hold non più necessario (carrello svuotato, prenotazione annullata).
    @PostMapping("/holds/{holdId}/release")
    void releaseHold(@PathVariable("holdId") String holdId);
}
