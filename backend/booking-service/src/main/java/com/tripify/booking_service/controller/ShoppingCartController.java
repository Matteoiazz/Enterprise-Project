package com.tripify.booking_service.controller;

import com.tripify.booking_service.dto.ActionResultDTO;
import com.tripify.booking_service.dto.AddToCartRequestDTO;
import com.tripify.booking_service.dto.CartDTO;
import com.tripify.booking_service.service.ShoppingCartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class ShoppingCartController {

    private final ShoppingCartService cartService;

    // userId letto dal claim "sub" del JWT già verificato da Spring Security,
    // non più dall'header X-User-Id (era falsificabile da chiunque avesse un
    // JWT valido, anche per leggere/svuotare il carrello di un altro utente).
    @GetMapping
    public ResponseEntity<CartDTO> getCart(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(cartService.getCartDTOForUser(jwt.getSubject()));
    }

    @PostMapping("/add")
    public ResponseEntity<ActionResultDTO> addItemToCart(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddToCartRequestDTO request) {

        cartService.addItem(jwt.getSubject(), request);
        return ResponseEntity.ok(new ActionResultDTO(true, "Elemento aggiunto al carrello con successo"));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ActionResultDTO> clearCart(@AuthenticationPrincipal Jwt jwt) {
        cartService.clearCart(jwt.getSubject());
        return ResponseEntity.ok(new ActionResultDTO(true, "Carrello svuotato con successo"));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ActionResultDTO> removeItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long itemId) {
        cartService.removeItem(jwt.getSubject(), itemId);
        return ResponseEntity.ok(new ActionResultDTO(true, "Articolo rimosso dal carrello"));
    }
}
