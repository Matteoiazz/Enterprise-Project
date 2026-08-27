package com.tripify.catalog_service.service;

import com.tripify.catalog_service.entity.CatalogItem;
import com.tripify.catalog_service.dto.CatalogItemDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CatalogService {

    List<CatalogItemDTO> getAllItems();

    CatalogItemDTO getItemById(Long id);

    // Firma estesa con i filtri, paginata
    Page<CatalogItemDTO> search(
            String category,
            String query,
            BigDecimal maxPrice,
            Integer minRating,
            String destination,
            String departure,
            Boolean guideIncluded,
            List<String> amenities,
            Boolean directOnly,
            LocalDate departureDate,
            Integer minSeats,
            LocalDate checkIn,
            LocalDate checkOut,
            Integer rooms,
            Pageable pageable
    );

    List<CatalogItem> getItemsByHost(UUID hostId);

    CatalogItem saveItem(CatalogItem item);

    /** Entità grezza (non mappata a DTO): serve per i controlli di proprietà e per l'update. */
    CatalogItem getRawItemById(Long id);

    /** Disattiva l'item (soft delete: vedi @SQLDelete/@SQLRestriction su CatalogItem). */
    void deactivateItem(Long id);

    List<String> getCitySuggestions(String query);
}
