package com.tripify.user_auth_service.repository;

import com.tripify.user_auth_service.entity.Companion;
import com.tripify.user_auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CompanionRepository extends JpaRepository<Companion, UUID> {
    List<Companion> findByUser(User user);
}