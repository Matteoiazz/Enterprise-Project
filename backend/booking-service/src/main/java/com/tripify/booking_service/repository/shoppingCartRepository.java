package com.tripify.booking_service.repository;

import com.tripify.booking_service.entity.shoppingCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface shoppingCartRepository extends JpaRepository<shoppingCart, Long>{
    //trova il carrello di un utente
    Optional<shoppingCart> findByUserId(String userId);
}
