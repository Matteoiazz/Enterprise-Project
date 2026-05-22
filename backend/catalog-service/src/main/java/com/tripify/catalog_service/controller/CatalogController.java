package com.tripify.catalog_service.controller;

import com.tripify.catalog_service.entity.CatalogItem;
import com.tripify.catalog_service.entity.Flight;
import com.tripify.catalog_service.entity.Hotel;
import com.tripify.catalog_service.entity.Itinerary;
import com.tripify.catalog_service.service.CatalogService;
import com.tripify.catalog_service.service.ItineraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    // 2. Motore di ricerca (es. /api/v1/catalog/items/search?keyword=Parigi)
    @GetMapping("/items/search")
    public ResponseEntity<List<CatalogItem>> searchItems(@RequestParam String keyword) {
        return ResponseEntity.ok(catalogService.searchItems(keyword));
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
    // Ricordati di aggiungere questo import in alto, insieme agli altri:
    // import com.tripify.catalog_service.entity.Flight;
    // import org.springframework.http.HttpStatus;

    // 5. Crea un nuovo Volo
    @PostMapping("/items/flights")
    public ResponseEntity<Flight> createFlight(@RequestBody Flight flight) {
        // Riceve il JSON da Postman, lo trasforma in un oggetto Flight e lo salva
        Flight savedFlight = (Flight) catalogService.saveItem(flight);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedFlight);
    }
    @PostMapping("/items/hotels")
    public ResponseEntity<Hotel> createHotel(@RequestBody Hotel hotel) {
        Hotel savedHotel = (Hotel) catalogService.saveItem(hotel);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedHotel);
    }
}