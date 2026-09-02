package com.tripify.user_auth_service.repository;

import com.tripify.user_auth_service.entity.TravelDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface TravelDocumentRepository extends JpaRepository<TravelDocument, UUID> {
    List<TravelDocument> findByUser_Id(UUID userId);
}
