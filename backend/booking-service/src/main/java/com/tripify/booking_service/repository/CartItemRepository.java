package com.tripify.booking_service.repository;

import com.tripify.booking_service.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long>{
    // Articoli il cui hold di 15 minuti è scaduto (vedi ShoppingCartService.purgeExpiredCartItems).
    List<CartItem> findByAddedAtBefore(LocalDateTime threshold);
}
