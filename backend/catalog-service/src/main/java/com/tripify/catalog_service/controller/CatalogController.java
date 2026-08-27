package com.tripify.catalog_service.controller;

import com.tripify.catalog_service.entity.Activity;
import com.tripify.catalog_service.entity.CatalogItem;
import com.tripify.catalog_service.entity.Flight;
import com.tripify.catalog_service.entity.Hotel;
import com.tripify.catalog_service.exception.AccessDeniedException;
import com.tripify.catalog_service.mapper.CatalogMapper;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
    private final CatalogMapper catalogMapper;

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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false) Integer rooms,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<CatalogItemDTO> results = catalogService.search(
                category, query, maxPrice, minRating, destination, departure, guideIncluded, amenities, directOnly,
                departureDate, minSeats, checkIn, checkOut, rooms, pageable
        );
        return ResponseEntity.ok(results);
    }

    @PostMapping("/items/flights")
    public ResponseEntity<CatalogItemDTO> createFlight(@Valid @RequestBody Flight flight, @AuthenticationPrincipal Jwt jwt) {
        flight.setHostId(hostIdOf(jwt));
        flight.setUserGenerated(true);
        flight.getFareClasses().forEach(fareClass -> fareClass.setFlight(flight));
        Flight savedFlight = (Flight) catalogService.saveItem(flight);
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogMapper.toDto(savedFlight));
    }

    @PostMapping("/items/hotels")
    public ResponseEntity<CatalogItemDTO> createHotel(@Valid @RequestBody Hotel hotel, @AuthenticationPrincipal Jwt jwt) {
        hotel.setHostId(hostIdOf(jwt));
        hotel.setUserGenerated(true);
        hotel.getRoomTypes().forEach(roomType -> roomType.setHotel(hotel));
        Hotel savedHotel = (Hotel) catalogService.saveItem(hotel);
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogMapper.toDto(savedHotel));
    }

    @PostMapping("/items/activities")
    public ResponseEntity<CatalogItemDTO> createActivity(@Valid @RequestBody Activity activity, @AuthenticationPrincipal Jwt jwt) {
        activity.setHostId(hostIdOf(jwt));
        activity.setUserGenerated(true);
        Activity savedActivity = (Activity) catalogService.saveItem(activity);
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogMapper.toDto(savedActivity));
    }

    /**
     * Modifica i campi descrittivi/di base di un annuncio già esistente. Tariffe e
     * camere (fareClasses/roomTypes) NON si toccano da qui apposta: sono referenziate
     * da hold/prenotazioni già aperti (FK da seat_holds/room_holds), sostituirle in
     * blocco le romperebbe. La gestione di tariffe/camere è una funzionalità a parte.
     */
    @PutMapping("/items/flights/{id}")
    public ResponseEntity<CatalogItemDTO> updateFlight(@PathVariable Long id, @Valid @RequestBody Flight incoming, @AuthenticationPrincipal Jwt jwt) {
        Flight existing = (Flight) requireOwnedItem(id, jwt);
        existing.setTitle(incoming.getTitle());
        existing.setDescription(incoming.getDescription());
        existing.setPrice(incoming.getPrice());
        existing.setCurrency(incoming.getCurrency());
        existing.setCategory(incoming.getCategory());
        existing.setRating(incoming.getRating());
        existing.setDepartureAirport(incoming.getDepartureAirport());
        existing.setArrivalAirport(incoming.getArrivalAirport());
        existing.setDepartureCity(incoming.getDepartureCity());
        existing.setArrivalCity(incoming.getArrivalCity());
        existing.setDepartureTime(incoming.getDepartureTime());
        existing.setArrivalTime(incoming.getArrivalTime());
        existing.setTotalSeats(incoming.getTotalSeats());
        existing.setStops(incoming.getStops());
        return ResponseEntity.ok(catalogMapper.toDto(catalogService.saveItem(existing)));
    }

    @PutMapping("/items/hotels/{id}")
    public ResponseEntity<CatalogItemDTO> updateHotel(@PathVariable Long id, @Valid @RequestBody Hotel incoming, @AuthenticationPrincipal Jwt jwt) {
        Hotel existing = (Hotel) requireOwnedItem(id, jwt);
        existing.setTitle(incoming.getTitle());
        existing.setDescription(incoming.getDescription());
        existing.setPrice(incoming.getPrice());
        existing.setCurrency(incoming.getCurrency());
        existing.setCategory(incoming.getCategory());
        existing.setRating(incoming.getRating());
        existing.setLocationLat(incoming.getLocationLat());
        existing.setLocationLng(incoming.getLocationLng());
        existing.setAddress(incoming.getAddress());
        existing.setCity(incoming.getCity());
        existing.setAmenities(incoming.getAmenities());
        return ResponseEntity.ok(catalogMapper.toDto(catalogService.saveItem(existing)));
    }

    @PutMapping("/items/activities/{id}")
    public ResponseEntity<CatalogItemDTO> updateActivity(@PathVariable Long id, @Valid @RequestBody Activity incoming, @AuthenticationPrincipal Jwt jwt) {
        Activity existing = (Activity) requireOwnedItem(id, jwt);
        existing.setTitle(incoming.getTitle());
        existing.setDescription(incoming.getDescription());
        existing.setPrice(incoming.getPrice());
        existing.setCurrency(incoming.getCurrency());
        existing.setCategory(incoming.getCategory());
        existing.setRating(incoming.getRating());
        existing.setActivityType(incoming.getActivityType());
        existing.setDuration(incoming.getDuration());
        existing.setMeetingPoint(incoming.getMeetingPoint());
        existing.setCity(incoming.getCity());
        existing.setMaxParticipants(incoming.getMaxParticipants());
        existing.setGuideIncluded(incoming.isGuideIncluded());
        return ResponseEntity.ok(catalogMapper.toDto(catalogService.saveItem(existing)));
    }

    /** Disattiva l'annuncio (soft delete): sparisce dalla ricerca ma resta per chi l'ha già prenotato. */
    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        requireOwnedItem(id, jwt);
        catalogService.deactivateItem(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/items/mine")
    public ResponseEntity<List<CatalogItemDTO>> getMyItems(@AuthenticationPrincipal Jwt jwt) {
        List<CatalogItemDTO> items = catalogService.getItemsByHost(hostIdOf(jwt)).stream()
                .map(catalogMapper::toDto)
                .toList();
        return ResponseEntity.ok(items);
    }

    private UUID hostIdOf(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private CatalogItem requireOwnedItem(Long id, Jwt jwt) {
        CatalogItem item = catalogService.getRawItemById(id);
        if (!item.getHostId().equals(hostIdOf(jwt))) {
            throw new AccessDeniedException("Non sei il proprietario di questo annuncio");
        }
        return item;
    }

    @GetMapping("/cities")
    public ResponseEntity<List<String>> getCitySuggestions(@RequestParam String query) {
        if (query == null || query.trim().length() < 2) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(catalogService.getCitySuggestions(query.trim()));
    }

    @GetMapping("/{itemId}/price")
    public ResponseEntity<Double> getItemPrice(@PathVariable Long itemId) {
        BigDecimal price = catalogService.getItemById(itemId).getPrice();
        return ResponseEntity.ok(price != null ? price.doubleValue() : null);
    }
}
