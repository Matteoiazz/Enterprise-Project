package com.tripify.booking_service.service;

import com.tripify.booking_service.entity.CartItem;
import com.tripify.booking_service.entity.ShoppingCart;
import com.tripify.booking_service.repository.CartItemRepository;
import com.tripify.booking_service.repository.ShoppingCartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShoppingCartService {

    private final ShoppingCartRepository cartRepository;
    private final CartItemRepository itemRepository;

    // 1. Recupera il carrello di un utente (se non esiste, lo crea vuoto all'istante)
    public ShoppingCart getCartForUser(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    ShoppingCart newCart = ShoppingCart.builder()
                            .userId(userId)
                            .items(new ArrayList<>())
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    // 2. Aggiunge un elemento al carrello (o aggiorna la quantità se c'era già)
    @Transactional
    public void addItem(String userId, Long catalogItemId, Integer quantity, Double price) {
        ShoppingCart cart = getCartForUser(userId);

        // Controlla se l'oggetto (volo/hotel) è già presente nel carrello
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getCatalogItemId().equals(catalogItemId))
                .findFirst();

        if (existingItem.isPresent()) {
            // Se c'è già, aumenta la quantità
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            itemRepository.save(item);
        } else {
            // Se è nuovo, crea una riga nel carrello
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .catalogItemId(catalogItemId)
                    .quantity(quantity)
                    .price(price)
                    .build();
            itemRepository.save(newItem);
        }
    }

    // 3. Svuota completamente il carrello (utilissimo dopo il checkout)
    @Transactional
    public void clearCart(String userId) {
        ShoppingCart cart = getCartForUser(userId);
        itemRepository.deleteByCartId(cart.getId());
    }
}