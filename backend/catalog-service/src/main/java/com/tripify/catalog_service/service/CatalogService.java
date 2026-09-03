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

    CatalogItemDTO getItemById(Long id);

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

    /** Come getItemsByHost, ma include anche gli annunci disattivati: solo per il proprietario stesso. */
    List<CatalogItem> getAllItemsByHost(UUID hostId);

    CatalogItem saveItem(CatalogItem item);

    CatalogItem getRawItemById(Long id);

    void deactivateItem(Long id);

    void reactivateItem(Long id);

    List<String> getCitySuggestions(String query);

    CatalogItem addImages(Long itemId, List<String> imageUrls);

    CatalogItem removeImage(Long itemId, String imageUrl);

    CatalogItem updateRating(Long itemId, Double average, Integer count);
}
