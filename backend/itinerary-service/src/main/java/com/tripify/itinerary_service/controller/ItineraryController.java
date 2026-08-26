package com.tripify.itinerary_service.controller;

import com.tripify.itinerary_service.dto.CreateListRequestDTO;
import com.tripify.itinerary_service.dto.UpdateVisibilityRequestDTO;
import com.tripify.itinerary_service.entity.FavoriteList;
import com.tripify.itinerary_service.service.ItineraryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/itinerary")
@RequiredArgsConstructor
public class ItineraryController {

    private final ItineraryService service;

    @PostMapping
    public ResponseEntity<FavoriteList> create(@Valid @RequestBody CreateListRequestDTO request,
                                                @AuthenticationPrincipal Jwt jwt) {
        FavoriteList created = service.createList(request.name(), jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<Void> addItem(@PathVariable Long id, @RequestParam Long itemId,
                                         @AuthenticationPrincipal Jwt jwt) {
        service.addItemToList(id, itemId, jwt.getSubject());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/share")
    public ResponseEntity<Void> share(@PathVariable Long id, @RequestParam String userId,
                                       @AuthenticationPrincipal Jwt jwt) {
        service.shareList(id, userId, jwt.getSubject());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/mine")
    public ResponseEntity<List<FavoriteList>> getMyLists(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.getUserLists(jwt.getSubject()));
    }

    /** "Salvati": liste proprie + condivise + itinerari altrui a cui si è messo like. */
    @GetMapping("/saved")
    public ResponseEntity<List<FavoriteList>> getSavedLists(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.getSavedLists(jwt.getSubject()));
    }

    @PostMapping("/catalog-likes/{catalogItemId}")
    public ResponseEntity<Map<String, Boolean>> toggleCatalogItemLike(@PathVariable Long catalogItemId,
                                                                        @AuthenticationPrincipal Jwt jwt) {
        boolean liked = service.toggleCatalogItemLike(catalogItemId, jwt.getSubject());
        return ResponseEntity.ok(Map.of("liked", liked));
    }

    @GetMapping("/catalog-likes/mine")
    public ResponseEntity<List<Long>> getMyLikedCatalogItems(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.getLikedCatalogItemIds(jwt.getSubject()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FavoriteList> getById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        FavoriteList list = service.getAccessibleById(id, jwt.getSubject());
        service.applyLikedByMe(list, jwt.getSubject());
        return ResponseEntity.ok(list);
    }

    @PatchMapping("/{id}/visibility")
    public ResponseEntity<FavoriteList> updateVisibility(@PathVariable Long id,
                                                           @Valid @RequestBody UpdateVisibilityRequestDTO request,
                                                           @AuthenticationPrincipal Jwt jwt) {
        FavoriteList updated = service.setVisibility(id, request.visibility(), request.city(), jwt.getSubject());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Map<String, Boolean>> toggleLike(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        boolean liked = service.toggleLike(id, jwt.getSubject());
        return ResponseEntity.ok(Map.of("liked", liked));
    }

    @PostMapping("/{id}/booked")
    public ResponseEntity<Void> registerBookingAttempt(@PathVariable Long id) {
        service.registerBookingAttempt(id);
        return ResponseEntity.ok().build();
    }

    // --- Feed pubblico e link con capabilities: nessuna autenticazione richiesta ---

    @GetMapping("/public")
    public ResponseEntity<List<FavoriteList>> getPublicFeed(@RequestParam(required = false) String city,
                                                              @RequestParam(required = false) String sort,
                                                              @AuthenticationPrincipal Jwt jwt) {
        List<FavoriteList> feed = service.getPublicFeed(city, sort);
        service.applyLikedByMe(feed, jwt != null ? jwt.getSubject() : null);
        return ResponseEntity.ok(feed);
    }

    @GetMapping("/public/{publicToken}")
    public ResponseEntity<FavoriteList> getByPublicToken(@PathVariable String publicToken,
                                                          @AuthenticationPrincipal Jwt jwt) {
        FavoriteList list = service.getByPublicToken(publicToken);
        service.applyLikedByMe(list, jwt != null ? jwt.getSubject() : null);
        return ResponseEntity.ok(list);
    }
}
