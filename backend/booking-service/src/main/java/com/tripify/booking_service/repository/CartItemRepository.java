package com.tripify.booking_service.repository;

import com.tripify.booking_service.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long>{
    //svuota il carrello dopo il pagamento
    void deleteByCartId(Long cartId);
}
