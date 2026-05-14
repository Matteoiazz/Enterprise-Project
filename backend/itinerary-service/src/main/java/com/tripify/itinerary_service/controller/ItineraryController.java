package com.tripify.itinerary_service.controller;

import com.tripify.itinerary_service.entity.FavoriteList;
import com.tripify.itinerary_service.service.ItineraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/itineraries")
@RequiredArgsConstructor
public class ItineraryController {

    private final ItineraryService service;

    @PostMapping
    public ResponseEntity<FavoriteList> create(@RequestParam String name, @RequestParam Long ownerId) {
        return ResponseEntity.ok(service.createList(name, ownerId));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<Void> addItem(@PathVariable Long id, @RequestParam Long itemId) {
        service.addItemToList(id, itemId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/share")
    public ResponseEntity<Void> share(@PathVariable Long id, @RequestParam Long userId) {
        service.shareList(id, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<FavoriteList>> getMyLists(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getUserLists(userId));
    }
}