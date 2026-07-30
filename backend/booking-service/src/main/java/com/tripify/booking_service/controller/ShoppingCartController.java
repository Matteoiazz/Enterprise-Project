package com.tripify.booking_service.controller;

import com.tripify.booking_service.entity.ShoppingCart;
import com.tripify.booking_service.service.ShoppingCartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class ShoppingCartController {

    private final ShoppingCartService cartService; // SBLOCCATO!

    @GetMapping("/{userId}")
    public ResponseEntity<ShoppingCart> getCart(@PathVariable String userId) {
        return ResponseEntity.ok(cartService.getCartForUser(userId));
    }

    @PostMapping("/{userId}/add")
    public ResponseEntity<String> addItemToCart(
            @PathVariable String userId,
            @RequestParam Long catalogItemId,
            @RequestParam Integer quantity) { // PREZZO RIMOSSO DAI PARAMETRI

        // CHIAMATA AL SERVICE SENZA IL PREZZO (se lo calcola da solo tramite Feign!)
        cartService.addItem(userId, catalogItemId, quantity);
        return ResponseEntity.ok("Elemento aggiunto al carrello con successo");
    }

    @DeleteMapping("/{userId}/clear")
    public ResponseEntity<String> clearCart(@PathVariable String userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok("Carrello svuotato con successo");
    }
}