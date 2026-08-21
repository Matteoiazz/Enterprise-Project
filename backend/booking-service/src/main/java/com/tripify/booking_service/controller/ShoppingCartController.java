package com.tripify.booking_service.controller;

import com.tripify.booking_service.dto.AddToCartRequestDTO;
import com.tripify.booking_service.entity.ShoppingCart;
import com.tripify.booking_service.service.ShoppingCartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class ShoppingCartController {

    private final ShoppingCartService cartService;

    // userId arriva dall'header X-User-Id impostato dal Gateway, non dal path.
    @GetMapping
    public ResponseEntity<ShoppingCart> getCart(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(cartService.getCartForUser(userId));
    }

    // catalogItemId e quantity ora arrivano nel body come DTO, non più come
    // @RequestParam sciolti: più coerente con PaymentRequestDTO e più facile
    // da estendere in futuro senza cambiare la firma del metodo.
    @PostMapping("/add")
    public ResponseEntity<String> addItemToCart(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody AddToCartRequestDTO request) {

        cartService.addItem(userId, request.catalogItemId(), request.quantity());
        return ResponseEntity.ok("Elemento aggiunto al carrello con successo");
    }

    @DeleteMapping("/clear")
    public ResponseEntity<String> clearCart(@RequestHeader("X-User-Id") String userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok("Carrello svuotato con successo");
    }
}