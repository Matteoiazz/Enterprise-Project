package com.tripify.booking_service.repository;

import com.tripify.booking_service.entity.ShoppingCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, Long>{
    //trova il carrello di un utente
    Optional<ShoppingCart> findByUserId(String userId);
}
