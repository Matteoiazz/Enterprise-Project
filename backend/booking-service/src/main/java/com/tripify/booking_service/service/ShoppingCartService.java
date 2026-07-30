package com.tripify.booking_service.service;

import com.tripify.booking_service.client.CatalogClient;
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

    // INIETTIAMO IL CLIENT FEIGN PER PARLARE COL CATALOGO
    private final CatalogClient catalogClient;

    // 1. Recupera il carrello di un utente
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

    // 2. Aggiunge un elemento (NON CHIEDIAMO PIÙ IL PREZZO AD ANDROID!)
    @Transactional
    public void addItem(String userId, Long catalogItemId, Integer quantity) {
        ShoppingCart cart = getCartForUser(userId);

        // LA CHIAMATA DI SICUREZZA: Chiediamo il prezzo reale al microservizio Catalogo
        Double realPrice = catalogClient.getItemPrice(catalogItemId);

        if (realPrice == null) {
            throw new RuntimeException("Errore di sicurezza: Articolo non trovato nel catalogo!");
        }

        // Controlla se l'oggetto è già presente nel carrello
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getCatalogItemId().equals(catalogItemId))
                .findFirst();

        if (existingItem.isPresent()) {
            // Se c'è già, aumenta la quantità
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            itemRepository.save(item);
        } else {
            // Se è nuovo, crea una riga nel carrello usando il PREZZO REALE SICURO
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .catalogItemId(catalogItemId)
                    .quantity(quantity)
                    .priceAtAdded(realPrice)
                    .build();
            itemRepository.save(newItem);
        }
    }

    // 3. Svuota completamente il carrello
    @Transactional
    public void clearCart(String userId) {
        ShoppingCart cart = getCartForUser(userId);
        itemRepository.deleteByCartId(cart.getId());
    }
}