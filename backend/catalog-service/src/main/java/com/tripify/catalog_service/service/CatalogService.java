package com.tripify.catalog_service.service;

import com.tripify.catalog_service.entity.CatalogItem;
import com.tripify.catalog_service.dto.CatalogItemDTO; // Aggiunto per il DTO
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID; // Aggiunto per l'UUID

public interface CatalogService {

    // Trova tutti gli elementi nel catalogo
    List<CatalogItem> getAllItems();

    // IL NUOVO SUPER-MOTORE DI RICERCA
    List<CatalogItemDTO> search(String category, String query, BigDecimal maxPrice, Integer minRating);

    // Trova tutti gli elementi creati da un organizzatore specifico (AGGIORNATO A UUID)
    List<CatalogItem> getItemsByHost(UUID hostId);

    // Salva un nuovo elemento nel database
    CatalogItem saveItem(CatalogItem item);
}