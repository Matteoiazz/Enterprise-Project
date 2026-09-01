package com.tripify.booking_service.repository;

import com.tripify.booking_service.entity.ShoppingCart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, Long>{
    //trova il carrello di un utente
    Optional<ShoppingCart> findByUserId(String userId);

    // Come findByUserId ma con lock pessimistico, usata solo dal checkout per
    // serializzare due checkout concorrenti sullo stesso carrello. @Query
    // esplicita perché altrimenti Spring Data legge "ForUpdate" come parte
    // del path della proprietà.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ShoppingCart c where c.userId = :userId")
    Optional<ShoppingCart> findByUserIdForUpdate(@Param("userId") String userId);
}
