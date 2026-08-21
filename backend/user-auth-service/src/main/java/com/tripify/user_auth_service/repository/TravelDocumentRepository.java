package com.tripify.user_auth_service.repository;

import com.tripify.user_auth_service.entity.TravelDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TravelDocumentRepository extends JpaRepository<TravelDocument, UUID> {
    List<TravelDocument> findByUser_Id(UUID userId);
}