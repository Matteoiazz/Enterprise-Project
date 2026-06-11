package com.tripify.catalog_service.repository;

import com.tripify.catalog_service.entity.CatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID; // Aggiunto per l'UUID

@Repository
public interface CatalogItemRepository extends JpaRepository<CatalogItem, Long>, JpaSpecificationExecutor<CatalogItem> {

    // Trova tutti gli oggetti creati da un determinato Organizzatore (AGGIORNATO A UUID)
    List<CatalogItem> findByHostId(UUID hostId);

    // NOTA SUL SOFT DELETE: Non devi preoccuparti di filtrare per "is_active = true"!
    // Grazie all'annotazione @SQLRestriction che hai messo sull'Entity,
    // Spring Boot lo aggiungerà in automatico a TUTTE queste query!
}