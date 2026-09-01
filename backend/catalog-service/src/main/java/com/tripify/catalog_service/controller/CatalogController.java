package com.tripify.catalog_service.controller;

import com.tripify.catalog_service.entity.Activity;
import com.tripify.catalog_service.entity.CatalogItem;
import com.tripify.catalog_service.entity.FareClass;
import com.tripify.catalog_service.entity.Flight;
import com.tripify.catalog_service.entity.Hotel;
import com.tripify.catalog_service.entity.RoomType;
import com.tripify.catalog_service.exception.AccessDeniedException;
import com.tripify.catalog_service.mapper.CatalogMapper;
import com.tripify.catalog_service.service.CatalogService;
import com.tripify.catalog_service.dto.CatalogItemDTO;
import com.tripify.catalog_service.dto.request.CreateActivityRequestDTO;
import com.tripify.catalog_service.dto.request.CreateFlightRequestDTO;
import com.tripify.catalog_service.dto.request.CreateHotelRequestDTO;
import com.tripify.catalog_service.dto.request.FareClassRequestDTO;
import com.tripify.catalog_service.dto.request.RoomTypeRequestDTO;
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

    /**
     * Il body è un DTO dedicato, non l'entità: id/hostId/isActive/isUserGenerated/rating
     * non sono campi accettabili dal client, li decide sempre il server qui sotto. Senza
     * questo, un id nel body farebbe merge() su un annuncio altrui invece di crearne uno
     * nuovo (save() su un'entità con id esistente è un UPDATE, non un INSERT).
     */
    @PostMapping("/items/flights")
    public ResponseEntity<CatalogItemDTO> createFlight(@Valid @RequestBody CreateFlightRequestDTO request, @AuthenticationPrincipal Jwt jwt) {
        Flight flight = new Flight();
        flight.setHostId(hostIdOf(jwt));
        flight.setUserGenerated(true);
        flight.setTitle(request.getTitle());
        flight.setDescription(request.getDescription());
        flight.setPrice(request.getPrice());
        flight.setCurrency(request.getCurrency());
        flight.setCategory(request.getCategory());
        flight.setDepartureAirport(request.getDepartureAirport());
        flight.setArrivalAirport(request.getArrivalAirport());
        flight.setDepartureCity(request.getDepartureCity());
        flight.setArrivalCity(request.getArrivalCity());
        flight.setDepartureTime(request.getDepartureTime());
        flight.setArrivalTime(request.getArrivalTime());
        flight.setTotalSeats(request.getTotalSeats());
        flight.setStops(request.getStops());
        for (FareClassRequestDTO fc : request.getFareClasses()) {
            FareClass fareClass = new FareClass();
            fareClass.setName(fc.getName());
            fareClass.setPrice(fc.getPrice());
            fareClass.setTotalSeats(fc.getTotalSeats());
            fareClass.setFlight(flight);
            flight.getFareClasses().add(fareClass);
        }
        Flight savedFlight = (Flight) catalogService.saveItem(flight);
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogMapper.toDto(savedFlight));
    }

    @PostMapping("/items/hotels")
    public ResponseEntity<CatalogItemDTO> createHotel(@Valid @RequestBody CreateHotelRequestDTO request, @AuthenticationPrincipal Jwt jwt) {
        Hotel hotel = new Hotel();
        hotel.setHostId(hostIdOf(jwt));
        hotel.setUserGenerated(true);
        hotel.setTitle(request.getTitle());
        hotel.setDescription(request.getDescription());
        hotel.setPrice(request.getPrice());
        hotel.setCurrency(request.getCurrency());
        hotel.setCategory(request.getCategory());
        hotel.setLocationLat(request.getLocationLat());
        hotel.setLocationLng(request.getLocationLng());
        hotel.setAddress(request.getAddress());
        hotel.setCity(request.getCity());
        hotel.setAmenities(request.getAmenities());
        for (RoomTypeRequestDTO rt : request.getRoomTypes()) {
            RoomType roomType = new RoomType();
            roomType.setName(rt.getName());
            roomType.setDescription(rt.getDescription());
            roomType.setPrice(rt.getPrice());
            roomType.setTotalRooms(rt.getTotalRooms());
            roomType.setMaxOccupancy(rt.getMaxOccupancy());
            roomType.setBenefits(rt.getBenefits());
            roomType.setImageUrls(rt.getImageUrls());
            roomType.setHotel(hotel);
            hotel.getRoomTypes().add(roomType);
        }
        Hotel savedHotel = (Hotel) catalogService.saveItem(hotel);
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogMapper.toDto(savedHotel));
    }

    @PostMapping("/items/activities")
    public ResponseEntity<CatalogItemDTO> createActivity(@Valid @RequestBody CreateActivityRequestDTO request, @AuthenticationPrincipal Jwt jwt) {
        Activity activity = new Activity();
        activity.setHostId(hostIdOf(jwt));
        activity.setUserGenerated(true);
        activity.setTitle(request.getTitle());
        activity.setDescription(request.getDescription());
        activity.setPrice(request.getPrice());
        activity.setCurrency(request.getCurrency());
        activity.setCategory(request.getCategory());
        activity.setActivityType(request.getActivityType());
        activity.setDuration(request.getDuration());
        activity.setMeetingPoint(request.getMeetingPoint());
        activity.setCity(request.getCity());
        activity.setMaxParticipants(request.getMaxParticipants());
        activity.setGuideIncluded(request.isGuideIncluded());
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
        Flight existing = requireOwnedItem(id, jwt, Flight.class);
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
        Hotel existing = requireOwnedItem(id, jwt, Hotel.class);
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
        Activity existing = requireOwnedItem(id, jwt, Activity.class);
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

    /** Come requireOwnedItem, ma verifica anche che l'annuncio sia del tipo atteso prima del cast. */
    private <T extends CatalogItem> T requireOwnedItem(Long id, Jwt jwt, Class<T> type) {
        CatalogItem item = requireOwnedItem(id, jwt);
        if (!type.isInstance(item)) {
            throw new IllegalArgumentException("Questo annuncio non è di tipo " + type.getSimpleName());
        }
        return type.cast(item);
    }

    @GetMapping("/cities")
    public ResponseEntity<List<String>> getCitySuggestions(@RequestParam String query) {
        if (query == null || query.trim().length() < 2) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(catalogService.getCitySuggestions(query.trim()));
    }

    @GetMapping("/items/host/{hostId}")
    public ResponseEntity<List<CatalogItemDTO>> getItemsByHost(@PathVariable UUID hostId) {
        List<CatalogItemDTO> items = catalogService.getItemsByHost(hostId).stream()
                .map(catalogMapper::toDto)
                .toList();
        return ResponseEntity.ok(items);
    }

}
