package com.tripify.catalog_service.controller;

import com.tripify.catalog_service.entity.CatalogItem;
import com.tripify.catalog_service.entity.Itinerary;
import com.tripify.catalog_service.service.CatalogService;
import com.tripify.catalog_service.service.ItineraryService;
import lombok.RequiredArgsConstructor;
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
}