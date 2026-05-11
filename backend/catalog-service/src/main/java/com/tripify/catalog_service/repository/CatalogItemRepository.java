package com.tripify.catalog_service.repository;

import com.tripify.catalog_service.entity.CatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CatalogItemRepository extends JpaRepository<CatalogItem, Long> {

    // Trova tutti gli oggetti creati da un determinato Organizzatore (Host)
    List<CatalogItem> findByHostId(Long hostId);

    // Ricerca generica per titolo (es. l'utente cerca "Parigi" nella barra di ricerca)
    List<CatalogItem> findByTitleContainingIgnoreCase(String keyword);

    // NOTA SUL SOFT DELETE: Non devi preoccuparti di filtrare per "is_active = true"!
    // Grazie all'annotazione @SQLRestriction che hai messo sull'Entity,
    // Spring Boot lo aggiungerà in automatico a TUTTE queste query!
}