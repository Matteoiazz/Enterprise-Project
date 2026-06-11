package com.tripify.catalog_service.controller;

import com.tripify.catalog_service.entity.CatalogItem;
import com.tripify.catalog_service.entity.Flight;
import com.tripify.catalog_service.entity.Hotel;
import com.tripify.catalog_service.entity.Itinerary;
import com.tripify.catalog_service.service.CatalogService;
import com.tripify.catalog_service.service.ItineraryService;
import com.tripify.catalog_service.dto.CatalogItemDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog") // L'URL base per questo microservizio
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;
    private final ItineraryService itineraryService;

    // 1. Ottieni tutto il catalogo (voli, hotel, attività)
    @GetMapping("/items")
    public ResponseEntity<List<CatalogItem>> getAllItems() {
        return ResponseEntity.ok(catalogService.getAllItems());
    }

    // 2. Super Motore di Ricerca Avanzato con DTO e Specifications
    @GetMapping("/items/search")
    public ResponseEntity<List<CatalogItemDTO>> searchCatalog(
            @RequestParam(required = false, defaultValue = "Tutti") String category,
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "0") Integer minRating
    ) {
        List<CatalogItemDTO> results = catalogService.search(category, query, maxPrice, minRating);
        return ResponseEntity.ok(results);
    }

    // 3. Ottieni i pacchetti commerciali in vendita
    @GetMapping("/packages")
    public ResponseEntity<List<Itinerary>> getCommercialPackages() {
        return ResponseEntity.ok(itineraryService.getAllCommercialPackages());
    }

    // 4. (Opzionale per testare) Crea una finta lista preferiti
    @PostMapping("/lists")
    public ResponseEntity<Itinerary> createList(
            @RequestParam String title,
            @RequestParam Long travelerId,
            @RequestParam boolean isPrivate) {
        return ResponseEntity.ok(itineraryService.createFavoriteList(title, travelerId, isPrivate));
    }

    // 5. Crea un nuovo Volo
    @PostMapping("/items/flights")
    public ResponseEntity<Flight> createFlight(
            @RequestBody Flight flight,
            @RequestHeader("X-User-Id") String userId) {

        // Convertiamo la stringa in UUID e la assegniamo
        flight.setHostId(UUID.fromString(userId));

        Flight savedFlight = (Flight) catalogService.saveItem(flight);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedFlight);
    }

    // 6. Crea un nuovo Hotel
    @PostMapping("/items/hotels")
    public ResponseEntity<Hotel> createHotel(
            @RequestBody Hotel hotel,
            @RequestHeader("X-User-Id") String userId) {

        // Convertiamo la stringa in UUID e la assegniamo
        hotel.setHostId(UUID.fromString(userId));

        Hotel savedHotel = (Hotel) catalogService.saveItem(hotel);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedHotel);
    }
}