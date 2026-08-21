package com.tripify.user_auth_service.repository;

import com.tripify.user_auth_service.entity.PaymentMethod;
import com.tripify.user_auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {
    List<PaymentMethod> findByUser(User user);
}