package com.tripify.catalog_service.controller;

import com.tripify.catalog_service.entity.Activity;
import com.tripify.catalog_service.entity.Flight;
import com.tripify.catalog_service.entity.Hotel;
import com.tripify.catalog_service.service.CatalogService;
import com.tripify.catalog_service.dto.CatalogItemDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/items")
    public ResponseEntity<List<CatalogItemDTO>> getAllItems() {
        return ResponseEntity.ok(catalogService.getAllItems());
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<CatalogItemDTO> getItemById(@PathVariable Long id) {
        return ResponseEntity.ok(catalogService.getItemById(id));
    }

    @GetMapping("/items/search")
    public ResponseEntity<Page<CatalogItemDTO>> searchCatalog(
            @RequestParam(required = false, defaultValue = "Tutti") String category,
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "0") Integer minRating,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String departure,
            @RequestParam(required = false) Boolean guideIncluded,
            @RequestParam(required = false) List<String> amenities,
            @RequestParam(required = false) Boolean directOnly,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,
            @RequestParam(required = false) Integer minSeats,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<CatalogItemDTO> results = catalogService.search(
                category, query, maxPrice, minRating, destination, departure, guideIncluded, amenities, directOnly, departureDate, minSeats, pageable
        );
        return ResponseEntity.ok(results);
    }

    @PostMapping("/items/flights")
    public ResponseEntity<Flight> createFlight(
            @Valid @RequestBody Flight flight,
            @RequestHeader("X-User-Id") String userId) {
        flight.setHostId(UUID.fromString(userId));
        Flight savedFlight = (Flight) catalogService.saveItem(flight);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedFlight);
    }

    @PostMapping("/items/hotels")
    public ResponseEntity<Hotel> createHotel(
            @Valid @RequestBody Hotel hotel,
            @RequestHeader("X-User-Id") String userId) {
        hotel.setHostId(UUID.fromString(userId));
        Hotel savedHotel = (Hotel) catalogService.saveItem(hotel);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedHotel);
    }

    @PostMapping("/items/activities")
    public ResponseEntity<Activity> createActivity(
            @Valid @RequestBody Activity activity,
            @RequestHeader("X-User-Id") String userId) {
        activity.setHostId(UUID.fromString(userId));
        Activity savedActivity = (Activity) catalogService.saveItem(activity);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedActivity);
    }

    @GetMapping("/cities")
    public ResponseEntity<List<String>> getCitySuggestions(@RequestParam String query) {
        if (query == null || query.trim().length() < 2) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(catalogService.getCitySuggestions(query.trim()));
    }
}
