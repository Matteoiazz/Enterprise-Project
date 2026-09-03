package com.tripify.user_auth_service.repository;

import com.tripify.user_auth_service.entity.Role;
import com.tripify.user_auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByUsername(String username);

    List<User> findByUsernameIn(Collection<String> usernames);

    List<User> findByRole(Role role);
}
