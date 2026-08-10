package com.tripify.catalog_service.service;

import com.tripify.catalog_service.entity.CatalogItem;
import com.tripify.catalog_service.dto.CatalogItemDTO;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CatalogService {

    List<CatalogItem> getAllItems();

    // Firma estesa con i filtri
    List<CatalogItemDTO> search(
            String category,
            String query,
            BigDecimal maxPrice,
            Integer minRating,
            String destination,
            String departure,
            Boolean guideIncluded,
            List<String> amenities,
            Boolean directOnly
    );

    List<CatalogItem> getItemsByHost(UUID hostId);

    CatalogItem saveItem(CatalogItem item);
}